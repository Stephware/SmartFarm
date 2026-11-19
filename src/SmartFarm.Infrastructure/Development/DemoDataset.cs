using System.Security.Cryptography;
using System.Text;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Development;

// Fixed IDs make the entire transaction repeatable; never use these identities outside local demos.
public static class DemoDataset
{
    public const string Prefix = "AniGo demo v1";
    public static Guid Id(string key) => new(SHA256.HashData(Encoding.UTF8.GetBytes(Prefix + ":" + key))[..16]);

    public static bool IsAllowedTarget(string? environment, string server, string database) =>
        environment == "Development" &&
        (server.StartsWith("127.0.0.1,", StringComparison.Ordinal) || server == "localhost" ||
         server.StartsWith("localhost,", StringComparison.Ordinal)) &&
        (database == "SmartFarmDB" || database.StartsWith("SmartFarmEnrollmentBackend_", StringComparison.Ordinal));

    public static IReadOnlyList<object> Create(DateTime now)
    {
        now = DateTime.SpecifyKind(now, DateTimeKind.Utc);
        var today = DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(now,
            TimeZoneInfo.FindSystemTimeZoneById("Asia/Manila")));
        var rows = new List<object>();
        foreach (var role in new[] { "owner", "manager", "worker", "applicant" })
            rows.Add(new SmartFarmUser { Id = Id(role), UserName = $"anigo-demo-{role}@example.test",
                NormalizedUserName = $"ANIGO-DEMO-{role.ToUpperInvariant()}@EXAMPLE.TEST",
                Email = $"anigo-demo-{role}@example.test", NormalizedEmail = $"ANIGO-DEMO-{role.ToUpperInvariant()}@EXAMPLE.TEST",
                FullName = $"{Prefix} {role}", SecurityStamp = Guid.NewGuid().ToString(),
                ConcurrencyStamp = Guid.NewGuid().ToString(), LockoutEnabled = true });
        for (var n = 0; n < 3; n++)
            rows.Add(new Farm { Id = Id($"farm{n}"), OwnerUserId = Id("owner"), JoinCode = $"DEMO{n}A71B2C3",
                Name = $"{Prefix}: {new[] { "Laguna vegetables", "Batangas orchard", "Empty farm" }[n]}",
                LocationText = "Synthetic demonstration farm, Philippines", Latitude = 14.17m, Longitude = 121.24m,
                CreatedAtUtc = now.AddDays(-80) });
        foreach (var n in new[] { 0, 1 })
            foreach (var role in new[] { "manager", "worker" })
                rows.Add(new FarmMember { Id = Id($"member{n}{role}"), FarmId = Id($"farm{n}"),
                    UserId = Id(role), Role = role == "manager" ? (byte)1 : (byte)2, JoinedAtUtc = now.AddDays(-70) });
        rows.Add(new FarmJoinRequest { Id = Id("pending"), FarmId = Id("farm0"), UserId = Id("applicant"), RequestedAtUtc = now.AddDays(-1) });
        rows.Add(new CropType { Id = Id("type"), Name = Prefix + " pechay", DefaultGrowthDays = 45,
            DefaultWateringIntervalDays = 2, DefaultHarvestUnit = "kg", CreatedAtUtc = now.AddDays(-80) });
        for (var n = 0; n < 4; n++)
        {
            rows.Add(new Plot { Id = Id($"plot{n}"), FarmId = Id(n == 3 ? "farm1" : "farm0"), Name = $"Demo bed {n + 1}",
                AreaSqMeters = 150, CreatedAtUtc = now.AddDays(-80) });
            var status = new byte[] { 1, 0, 2, 1 }[n];
            rows.Add(new CropCycle { Id = Id($"crop{n}"), PlotId = Id($"plot{n}"), CropTypeId = Id("type"),
                PlannedPlantingDate = today.AddDays(status == 0 ? 7 : -45), PlantedDate = status == 0 ? null : today.AddDays(-45),
                ExpectedHarvestDate = today.AddDays(status == 2 ? -1 : 14), Status = status, HarvestUnit = n == 3 ? "bunch" : "kg",
                WateringIntervalDays = 2, CompletedAtUtc = status == 2 ? now.AddDays(-1) : null, CreatedAtUtc = now.AddDays(-60) });
        }
        for (var n = 0; n < 4; n++)
            rows.Add(new FarmTask { Id = Id($"task{n}"), FarmId = Id("farm0"), CropCycleId = Id("crop0"),
                AssignedToUserId = Id("worker"), TaskType = n == 3 ? (byte)4 : (byte)2, DueDate = today.AddDays(n - 1),
                Status = n == 3 ? (byte)1 : (byte)0, Description = Prefix + " check irrigation and crop condition",
                DedupeKey = $"anigo-demo-v1:task:{n}", CreatedAtUtc = now.AddDays(-4),
                ClosedAtUtc = n == 3 ? now : null, ClosedByUserId = n == 3 ? Id("worker") : null });
        rows.Add(new CropActivity { Id = Id("activity"), CropCycleId = Id("crop0"), ActivityType = 2,
            OccurredAtUtc = now.AddDays(-1), RecordedAtUtc = now.AddDays(-1), RecordedByUserId = Id("worker"), Notes = Prefix + " compost application" });
        rows.Add(new CropObservation { Id = Id("observation"), CropCycleId = Id("crop0"), HealthStatus = 2,
            GrowthStage = "Vegetative", IssueCategory = 4, IssueSeverity = 1, FollowUpStatus = 1,
            ObservedAtUtc = now, RecordedAtUtc = now, RecordedByUserId = Id("worker"), Notes = Prefix + " inspect after forecast rain" });
        rows.Add(new InventoryItem { Id = Id("item"), FarmId = Id("farm0"), Name = Prefix + " compost", Category = 2,
            Unit = "kg", QuantityOnHand = 2, ReorderLevel = 5, MetadataVersion = Id("metadata"), CreatedAtUtc = now.AddDays(-30) });
        rows.Add(new InventoryTransaction { Id = Id("opening"), InventoryItemId = Id("item"), TransactionType = 1,
            QuantityChange = 10, OccurredAtUtc = now.AddDays(-30), RecordedAtUtc = now.AddDays(-30), RecordedByUserId = Id("owner"), Reason = Prefix });
        rows.Add(new InventoryTransaction { Id = Id("consumption"), InventoryItemId = Id("item"), CropActivityId = Id("activity"),
            TransactionType = 3, QuantityChange = -8, OccurredAtUtc = now.AddDays(-1), RecordedAtUtc = now.AddDays(-1), RecordedByUserId = Id("worker"), Reason = Prefix });
        for (var n = 0; n < 3; n++)
            rows.Add(new Harvest { Id = Id($"harvest{n}"), CropCycleId = Id(n == 2 ? "crop3" : "crop0"),
                Quantity = new[] { 12.5m, 6.25m, 10m }[n], Unit = n == 2 ? "bunch" : "kg",
                HarvestedAtUtc = now.AddDays(-n), RecordedAtUtc = now.AddDays(-n), RecordedByUserId = Id("worker"), Notes = Prefix + " partial harvest" });
        for (var n = 0; n < 2; n++)
        {
            rows.Add(new WeatherForecast { Id = Id($"forecast{n}"), FarmId = Id("farm0"), ForecastDate = today.AddDays(-n),
                RetrievedAtUtc = now.AddDays(-n), Provider = "SyntheticDemo", MinTemperatureC = 25, MaxTemperatureC = 34,
                RainfallMm = 55, MaxWindKph = 20, MaxPrecipitationProbability = 90 });
            rows.Add(new WeatherAdvisory { Id = Id($"advisory{n}"), FarmId = Id("farm0"), WeatherForecastId = Id($"forecast{n}"),
                AdvisoryDate = today.AddDays(-n), AdvisoryType = 1, Severity = 3, Status = (byte)n,
                CreatedAtUtc = now.AddDays(-n), ResolvedAtUtc = n == 1 ? now : null, Message = Prefix + ": synthetic forecast rain; check drainage." });
            rows.Add(new Notification { Id = Id($"notification{n}"), UserId = Id("owner"), WeatherAdvisoryId = Id($"advisory{n}"),
                Type = 2, DeduplicationKey = $"anigo-demo-v1:weather:{n}", Title = "Demo forecast advisory",
                Message = Prefix + ": synthetic warning, not an official emergency notice.", CreatedAtUtc = now.AddDays(-n), ReadAtUtc = n == 1 ? now : null });
        }
        rows.Add(new Notification { Id = Id("reminder"), UserId = Id("worker"), FarmTaskId = Id("task0"), Type = 1,
            DeduplicationKey = "anigo-demo-v1:reminder", Title = "Demo overdue watering", Message = Prefix, CreatedAtUtc = now });
        return rows;
    }
}
