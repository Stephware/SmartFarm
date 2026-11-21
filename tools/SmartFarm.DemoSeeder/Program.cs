using System.Diagnostics;
using System.Security.Cryptography;
using System.Text.Json;
using Microsoft.AspNetCore.Identity;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Development;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Infrastructure.Services.Monitoring;

// Explicit operator tool: never registered with Web/API/Worker startup.
var config = new ConfigurationBuilder().AddUserSecrets("SmartFarm.Web.Development").AddEnvironmentVariables().Build();
var cs = config.GetConnectionString("SmartFarm") ?? throw new InvalidOperationException("Configure ConnectionStrings__SmartFarm.");
var target = new SqlConnectionStringBuilder(cs);
if (!DemoDataset.IsAllowedTarget(Environment.GetEnvironmentVariable("DOTNET_ENVIRONMENT"), target.DataSource, target.InitialCatalog))
    throw new InvalidOperationException("Requires Development and an approved loopback SmartFarm database.");
Console.WriteLine($"Development target: {target.DataSource} / {target.InitialCatalog}");
await using var db = new SmartFarmDbContext(new DbContextOptionsBuilder<SmartFarmDbContext>().UseSqlServer(cs).Options);
var repo = Directory.GetCurrentDirectory();
if (!File.Exists(Path.Combine(repo, "database/verification/read_catalog.sql")))
    throw new InvalidOperationException("Run from the repository root.");
var scratch = Path.Combine(Path.GetTempPath(), "anigo-seed-" + Guid.NewGuid().ToString("N"));
Directory.CreateDirectory(scratch);
var expected = Path.Combine(scratch, "expected.sql");
var live = Path.Combine(scratch, "live.json");
await File.WriteAllTextAsync(expected, db.Database.GenerateCreateScript());
await db.Database.OpenConnectionAsync();
await using (var command = db.Database.GetDbConnection().CreateCommand())
{
    // The read-only catalog query's original SmartFarmDB guard is replaced only for the disposable test prefix.
    command.CommandText = (await File.ReadAllTextAsync(Path.Combine(repo, "database/verification/read_catalog.sql")))
        .Replace("IF DB_NAME() <> N'SmartFarmDB'", "IF DB_NAME() <> N'SmartFarmDB' AND DB_NAME() NOT LIKE N'SmartFarmEnrollmentBackend[_]%'");
    await using var reader = await command.ExecuteReaderAsync();
    var json = new System.Text.StringBuilder();
    while (await reader.ReadAsync()) json.Append(reader.GetString(0));
    await File.WriteAllTextAsync(live, json.ToString());
}
var start = new ProcessStartInfo("python3") { UseShellExecute = false };
foreach (var arg in new[] { "database/verification/verify_schema.py", "--source", expected, "--live-schema", live }) start.ArgumentList.Add(arg);
using (var process = Process.Start(start)!)
{
    await process.WaitForExitAsync();
    if (process.ExitCode != 0) throw new InvalidOperationException($"Schema differs; no seed applied. Private metadata: {scratch}");
}
if (!args.Contains("--apply")) { Console.WriteLine("Schema verified. Read-only preflight; no backup or seed applied."); return; }
string Required(string name)
{
    var index = Array.IndexOf(args, name);
    return index >= 0 && index + 1 < args.Length ? args[index + 1] : throw new ArgumentException($"Missing {name}.");
}
var credentials = Path.GetFullPath(Required("--credentials"));
if (File.Exists(credentials)) throw new InvalidOperationException("Credentials file must be new.");
var backup = $"/var/opt/mssql/backup/anigo-demo-{DateTime.UtcNow:yyyyMMddHHmmss}-{Guid.NewGuid():N}.bak";
await using (var command = db.Database.GetDbConnection().CreateCommand())
{
    command.CommandTimeout = 180;
    command.CommandText = $"BACKUP DATABASE [{target.InitialCatalog.Replace("]", "]]")}] TO DISK = @path WITH COPY_ONLY, CHECKSUM; RESTORE VERIFYONLY FROM DISK = @path WITH CHECKSUM;";
    var parameter = command.CreateParameter(); parameter.ParameterName = "@path"; parameter.Value = backup; command.Parameters.Add(parameter);
    await command.ExecuteNonQueryAsync();
}
Console.WriteLine($"Backup and VERIFYONLY succeeded: {backup} (not a restore drill).");
await using var transaction = await db.Database.BeginTransactionAsync(System.Data.IsolationLevel.Serializable);
if (await db.Farms.AnyAsync(f => f.Id == DemoDataset.Id("farm0")))
{ Console.WriteLine("Demo already present; no records overwritten."); return; }
var rows = DemoDataset.Create(DateTime.UtcNow);
var secrets = new Dictionary<string, string>();
foreach (var user in rows.OfType<SmartFarmUser>())
{
    var password = "Aa9!" + Convert.ToHexString(RandomNumberGenerator.GetBytes(20));
    user.PasswordHash = new PasswordHasher<SmartFarmUser>().HashPassword(user, password);
    secrets.Add(user.Email!, password);
}
string? photoKey = null;
LocalObservationPhotoStore? photoStore = null;
try
{
    if (args.Contains("--photo-source"))
    {
        var source = Required("--photo-source");
        var extension = Path.GetExtension(source).ToLowerInvariant();
        var bytes = await File.ReadAllBytesAsync(source);
        if (bytes.Length is 0 or > 5 * 1024 * 1024 || extension is not (".jpg" or ".png" or ".webp"))
            throw new ArgumentException("Provide a valid local JPG/PNG/WebP photo up to 5 MB.");
        photoStore = new LocalObservationPhotoStore(Required("--photo-root"));
        photoKey = await photoStore.SaveAsync(bytes, extension);
        db.ObservationPhotos.Add(new ObservationPhoto { Id = DemoDataset.Id("photo"), CropObservationId = DemoDataset.Id("observation"),
            StorageKey = photoKey, ContentType = extension == ".jpg" ? "image/jpeg" : "image/" + extension[1..],
            FileSizeBytes = bytes.Length, UploadedAtUtc = DateTime.UtcNow, UploadedByUserId = DemoDataset.Id("worker") });
    }
    db.AddRange(rows);
    await db.SaveChangesAsync();
    var fileOptions = new FileStreamOptions { Mode = FileMode.CreateNew, Access = FileAccess.Write };
    if (!OperatingSystem.IsWindows()) fileOptions.UnixCreateMode = UnixFileMode.UserRead | UnixFileMode.UserWrite;
    using (var output = new FileStream(credentials, fileOptions))
        await JsonSerializer.SerializeAsync(output, secrets);
    await transaction.CommitAsync();
}
catch
{
    if (photoKey is not null) await photoStore!.DeleteAsync(photoKey);
    throw;
}
Console.WriteLine($"Applied {rows.Count} identifiable synthetic demo records. Credentials written privately to {credentials}.");
