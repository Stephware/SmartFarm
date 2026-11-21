using Microsoft.AspNetCore.Identity;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Infrastructure.Services.Admin;

namespace SmartFarm.Web.Authentication;

// Operator-only, explicitly invoked from the server shell; never an HTTP endpoint.
public static class SuperAdminProvisioner
{
    public static bool IsExplicitBootstrap(string? flag, string[] args) =>
        flag == "I_UNDERSTAND_ADMIN_ACCESS" && args.Length == 2 &&
        args[0] == "--provision-super-admin" &&
        !string.IsNullOrWhiteSpace(args[1]);

    public static async Task RunAsync(IServiceProvider services, string[] args)
    {
        if (!IsExplicitBootstrap(Environment.GetEnvironmentVariable("SMARTFARM_ADMIN_BOOTSTRAP"), args))
            throw new InvalidOperationException(
                "Operator action blocked. Set SMARTFARM_ADMIN_BOOTSTRAP=I_UNDERSTAND_ADMIN_ACCESS " +
                "for this command only, and provide an existing user's email.");

        await using var scope = services.CreateAsyncScope();
        var users = scope.ServiceProvider.GetRequiredService<UserManager<SmartFarmUser>>();
        var roles = scope.ServiceProvider.GetRequiredService<RoleManager<SmartFarmRole>>();
        var db = scope.ServiceProvider.GetRequiredService<SmartFarmDbContext>();
        var audit = scope.ServiceProvider.GetRequiredService<AdminAuditWriter>();
        var user = await users.FindByEmailAsync(args[1].Trim())
            ?? throw new InvalidOperationException("Existing account not found.");
        if (user.DisabledAtUtc is not null)
            throw new InvalidOperationException("Cannot provision a disabled account.");

        if (await users.IsInRoleAsync(user, SuperAdminSecurity.Role))
        {
            Console.WriteLine("The account already has the Super Admin role; no changes made.");
            return;
        }

        await using var tx = await db.Database.BeginTransactionAsync();
        if (!await roles.RoleExistsAsync(SuperAdminSecurity.Role))
        {
            var created = await roles.CreateAsync(new SmartFarmRole { Name = SuperAdminSecurity.Role });
            if (!created.Succeeded)
                throw new InvalidOperationException("Could not provision the Super Admin role: " +
                    string.Join("; ", created.Errors.Select(e => e.Code)));
        }
        var result = await users.AddToRoleAsync(user, SuperAdminSecurity.Role);
        if (!result.Succeeded)
            throw new InvalidOperationException("Could not grant Super Admin: " +
                string.Join("; ", result.Errors.Select(e => e.Code)));
        await audit.RecordAsync(null, "bootstrap-cli", "super-admin-granted",
            "user", user.Id, "Explicit server operator provisioning");
        await tx.CommitAsync();
        Console.WriteLine("Super Admin role granted. Sign in and enroll an authenticator before accessing /admin.");
    }
}
