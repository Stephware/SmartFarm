using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using SmartFarm.Api.Controllers;
using System.Text.Json;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.Configuration;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Contracts.Reports;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Enums;
using Xunit;

namespace SmartFarm.Api.Tests;

// Set SMARTFARM_TEST_CONNECTION only for a separate, disposable SQL Server
// database initialized with SmartFarm's existing SQL scripts.
public sealed class BusinessWorkflowSqlTests
{
    [Fact]
    public async Task OwnerCanManageFarmToHarvestWorkflow_WhenTestDatabaseIsConfigured()
    {
        var connection = Environment.GetEnvironmentVariable("SMARTFARM_TEST_CONNECTION");
        if (string.IsNullOrWhiteSpace(connection))
            return; // Offline test runs do not require a SQL Server instance.

        var database = new SqlConnectionStringBuilder(connection).InitialCatalog;
        Assert.True(database.StartsWith("SmartFarmEnrollmentTest_", StringComparison.Ordinal) || database.StartsWith("SmartFarmEnrollmentBackend_", StringComparison.Ordinal), "SQL workflow tests require a separately initialized disposable enrollment database.");
        using var factory = new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
        {
            builder.ConfigureAppConfiguration((_, config) =>
                config.AddInMemoryCollection(new Dictionary<string, string?>
                {
                    ["ConnectionStrings:SmartFarm"] = connection
                }));
            builder.ConfigureServices(services =>
            {
                services.RemoveAll<SmartFarmDbContext>();
                services.RemoveAll<DbContextOptions<SmartFarmDbContext>>();
                services.AddDbContext<SmartFarmDbContext>(options => options.UseSqlServer(connection));
            });
        });
        await using (var scope = factory.Services.CreateAsyncScope())
            Assert.Equal(database, scope.ServiceProvider.GetRequiredService<SmartFarmDbContext>().Database.GetDbConnection().Database);
        using var client = factory.CreateClient();
        var suffix = Guid.NewGuid().ToString("N")[..10];
        var password = "SmartFarmTest2026!";
        var email = $"phase8-{suffix}@example.test";

        await RegisterAsync(client, email, password);
        await LoginAsync(client, email, password);

        var createdFarm = await PostAsync<FarmDetails>(client, "/api/farms", new FarmInput
        {
            Name = "Phase 8 Farm " + suffix,
            LocationText = "Test location",
            TimeZoneId = "Asia/Manila"
        });
        var farm = $"/api/farms/{createdFarm.Id}";
        Assert.Equal("Owner", createdFarm.Role);
        // Weather is available only to members and must work before the worker runs.
        var initialWeather = await GetAsync<SmartFarm.Core.Contracts.Weather.WeatherOverview>(
            client, $"/api/farms/{createdFarm.Id}/weather");
        Assert.False(initialWeather.HasCoordinates);
        Assert.Empty(initialWeather.Days);
        using (var invalidZone = await client.PostAsJsonAsync("/api/farms", new FarmInput
        {
            Name = "Invalid zone farm", LocationText = "Test location",
            TimeZoneId = "Not/ARealTimeZone"
        }))
            Assert.Equal(HttpStatusCode.BadRequest, invalidZone.StatusCode);

        var plot = await PostAsync<PlotDetails>(client, farm + "/plots", new PlotInput
        {
            Name = "Test Plot", AreaSqMeters = 100.00m
        });
        Assert.Equal(createdFarm.Id, plot.FarmId);

        var type = await PostAsync<CropTypeDetails>(client, farm + "/crop-types", new CropTypeInput
        {
            Name = "Phase 8 Crop " + suffix,
            DefaultHarvestUnit = "kg",
            DefaultGrowthDays = 60,
            DefaultWateringIntervalDays = 3
        });
        var planting = DateOnly.FromDateTime(DateTime.UtcNow);
        var planned = await PostAsync<CropCycleDetails>(
            client, farm + $"/plots/{plot.Id}/crop-cycles", new CropCyclePlanInput
            {
                CropTypeId = type.Id,
                PlannedPlantingDate = planting,
                ExpectedHarvestDate = planting.AddDays(60),
                WateringIntervalDays = 3
            });
        var cycle = farm + $"/crop-cycles/{planned.Id}";
        var plantingTask = await PostAsync<FarmTaskDetails>(client, farm + "/tasks",
            new FarmTaskInput(1, planting, "Plant the seedlings", planned.Id, null));
        var active = await PostAsync<CropCycleDetails>(client, cycle + "/transitions",
            new CropCycleTransitionInput
            {
                RowVersion = planned.RowVersion,
                Status = CropCycleStatus.Active,
                PlantedDate = planting
            });
        Assert.Equal(CropCycleStatus.Active, active.Status);
        Assert.Equal((byte)1, (await GetAsync<List<FarmTaskDetails>>(client, farm + "/tasks"))
            .Single(t => t.Id == plantingTask.Id).Status);

        var taskPath = farm + "/tasks";
        var assignedTask = await PostAsync<FarmTaskDetails>(client, taskPath,
            new FarmTaskInput(2, planting, "Water the new crop", planned.Id, null));
        Assert.Equal((byte)0, assignedTask.Status);
        Assert.Contains(await GetAsync<List<FarmTaskDetails>>(client, taskPath),
            t => t.Id == assignedTask.Id);

        using (var updateResponse = await client.PutAsJsonAsync(
            taskPath + $"/{assignedTask.Id}", new FarmTaskUpdateInput(
                planting.AddDays(1), "Water tomorrow", null, assignedTask.RowVersion)))
        {
            Assert.Equal(HttpStatusCode.OK, updateResponse.StatusCode);
            var updated = await updateResponse.Content.ReadFromJsonAsync<FarmTaskDetails>();
            Assert.NotNull(updated);
            Assert.Equal(planting.AddDays(1), updated.DueDate);
            Assert.Equal("Water tomorrow", updated.Description);
            using var stale = await client.PutAsJsonAsync(taskPath + $"/{assignedTask.Id}",
                new FarmTaskUpdateInput(planting, "Stale update", null, assignedTask.RowVersion));
            Assert.Equal(HttpStatusCode.Conflict, stale.StatusCode);
            assignedTask = updated;
        }
        using (var closedResponse = await client.PutAsJsonAsync(
            taskPath + $"/{assignedTask.Id}/close",
            new FarmTaskCloseInput(1, assignedTask.RowVersion)))
        {
            Assert.Equal(HttpStatusCode.OK, closedResponse.StatusCode);
            var completed = await closedResponse.Content.ReadFromJsonAsync<FarmTaskDetails>();
            Assert.Equal((byte)1, completed?.Status);
        }

        var linkedTask = await PostAsync<FarmTaskDetails>(client, taskPath,
            new FarmTaskInput(2, planting, "Linked watering", planned.Id, null));
        var monitoring = cycle + "/monitoring";
        var activity = await PostAsync<ActivityDetails>(client, monitoring + "/activities",
            new RecordActivityInput { ActivityType = 1, Notes = "Watered", FarmTaskId = linkedTask.Id });
        Assert.Equal(linkedTask.Id, activity.FarmTaskId);
        Assert.Equal((byte)1, (await GetAsync<List<FarmTaskDetails>>(client, taskPath))
            .Single(t => t.Id == linkedTask.Id).Status);
        var autoTask = await PostAsync<FarmTaskDetails>(client, taskPath,
            new FarmTaskInput(2, planting, "Automatic watering", planned.Id, null));
        var automatic = await PostAsync<ActivityDetails>(client, monitoring + "/activities",
            new RecordActivityInput { ActivityType = 1, Notes = "Watered again" });
        Assert.Equal(autoTask.Id, automatic.FarmTaskId);
        await PostAsync<ObservationDetails>(client, monitoring + "/observations",
            new RecordObservationInput { HealthStatus = 1, GrowthStage = "Growing" });
        var observations = await GetAsync<MonitoringDetails>(client, monitoring);
        Assert.Equal(2, observations.Activities.Count);
        Assert.Single(observations.Observations);

        // A lost photo-upload response must not create duplicate photos.
        var observationId = observations.Observations.Single().Id;
        var photoId = Guid.NewGuid();
        var photoRoute = monitoring + $"/observations/{observationId}/photos/{photoId}";
        var validPhoto = new byte[] { 0xff, 0xd8, 0xff, 0xe0, 0x01, 0x02, 0x03 };
        async Task<HttpResponseMessage> UploadPhoto(byte[] photoBytes)
        {
            using var form = new MultipartFormDataContent();
            using var image = new ByteArrayContent(photoBytes);
            image.Headers.ContentType = new System.Net.Http.Headers.MediaTypeHeaderValue("image/jpeg");
            form.Add(image, "photo", "field.jpg");
            return await client.PutAsync(photoRoute, form);
        }
        using (var firstUpload = await UploadPhoto(validPhoto))
        {
            Assert.Equal(HttpStatusCode.OK, firstUpload.StatusCode);
            var firstPhoto = await firstUpload.Content.ReadFromJsonAsync<PhotoDetails>();
            Assert.Equal(photoId, firstPhoto?.Id);
        }
        using (var replay = await UploadPhoto(validPhoto))
        {
            Assert.Equal(HttpStatusCode.OK, replay.StatusCode);
            Assert.Equal(photoId, (await replay.Content.ReadFromJsonAsync<PhotoDetails>())?.Id);
        }
        var withPhoto = await GetAsync<MonitoringDetails>(client, monitoring);
        Assert.Single(withPhoto.Observations.Single().Photos);
        using (var differentPhoto = await UploadPhoto(new byte[] { 0xff, 0xd8, 0xff, 0xe0, 0x09 }))
            Assert.Equal(HttpStatusCode.Conflict, differentPhoto.StatusCode);

        var inventory = farm + "/inventory";
        var item = await PostAsync<InventoryItemDetails>(client, inventory, new InventoryItemInput
        {
            Name = "Test Fertilizer", Category = InventoryCategory.Fertilizers,
            Unit = "kg", InitialQuantity = 5m, ReorderLevel = 2m
        });
        var itemPath = inventory + $"/{item.Id}";
        item = await PostAsync<InventoryItemDetails>(client, itemPath + "/transactions",
            new InventoryMovementInput
            {
                TransactionType = StockTransactionType.Receipt,
                QuantityChange = 2m,
                RowVersion = item.RowVersion
            });
        item = await PostAsync<InventoryItemDetails>(client, itemPath + "/transactions",
            new InventoryMovementInput
            {
                TransactionType = StockTransactionType.Consumption,
                QuantityChange = -1m,
                CropActivityId = activity.Id,
                RowVersion = item.RowVersion
            });
        Assert.Equal(6m, item.QuantityOnHand);
        var movements = await GetAsync<List<InventoryTransactionDetails>>(
            client, itemPath + "/transactions");
        Assert.Equal(3, movements.Count); // Opening + receipt + consumption.

        var harvestTask = await PostAsync<FarmTaskDetails>(client, taskPath,
            new FarmTaskInput(5, planting.AddDays(60), "Harvest the crop", planned.Id, null));
        var harvests = cycle + "/harvests";
        var first = await PostAsync<HarvestEntry>(client, harvests,
            new RecordHarvestInput { Quantity = 2m });
        Assert.Equal((byte)1, (await GetAsync<List<FarmTaskDetails>>(client, taskPath))
            .Single(t => t.Id == harvestTask.Id).Status);
        await PostAsync<HarvestEntry>(client, harvests,
            new RecordHarvestInput { Quantity = 1m });
        var beforeVoid = await GetAsync<HarvestOverview>(client, harvests);
        Assert.Equal(3m, beforeVoid.TotalQuantity);
        Assert.Equal(2, beforeVoid.ValidHarvestCount);

        await PostAsync<HarvestEntry>(client, harvests + $"/{first.Id}/void",
            new VoidHarvestInput { Reason = "Duplicate entry" });
        var afterVoid = await GetAsync<HarvestOverview>(client, harvests);
        Assert.Equal(1m, afterVoid.TotalQuantity);
        Assert.Equal(1, afterVoid.ValidHarvestCount);
        Assert.Equal(1, afterVoid.VoidedHarvestCount);

        // Farm-wide dashboard and reports use exactly the same harvest records.
        var farmReport = await GetAsync<HarvestReport>(client, farm + "/reports/harvests");
        Assert.Equal(1, farmReport.ValidHarvestCount);
        Assert.Equal(1, farmReport.VoidedHarvestCount);
        Assert.Equal(2, farmReport.Entries.Count);
        Assert.Equal(1m, farmReport.UnitTotals.Single().Quantity);
        Assert.Equal("kg", farmReport.UnitTotals.Single().Unit);
        Assert.Single(farmReport.CropPlotTotals);
        var filteredReport = await GetAsync<HarvestReport>(client,
            farm + $"/reports/harvests?plotId={plot.Id}&cropTypeId={type.Id}");
        Assert.Equal(1m, filteredReport.UnitTotals.Single().Quantity);
        using (var invalidReport = await client.GetAsync(
            farm + "/reports/harvests?from=2026-11-10&to=2026-01-01"))
            Assert.Equal(HttpStatusCode.BadRequest, invalidReport.StatusCode);
        var dashboard = await GetAsync<FarmDashboard>(client, farm + "/dashboard");
        Assert.Equal(1, dashboard.ActivePlots);
        Assert.Equal(1, dashboard.ActiveCrops);
        Assert.Equal(1m, dashboard.HarvestLast30Days.Single().Quantity);

        // Lost-response retries must not create extra inventory movements or harvests.
        var inventorySyncId = Guid.NewGuid();
        var inventorySync = new SyncWriteInput
        {
            FarmId = createdFarm.Id, TargetId = item.Id, OperationType = "inventory",
            Movement = new InventoryMovementInput
            {
                TransactionType = StockTransactionType.Receipt,
                QuantityChange = 1m, RowVersion = item.RowVersion
            }
        };
        var syncRoute = $"/api/sync/operations/{inventorySyncId}";
        var firstStockSync = await PostAsync<SyncWriteResult>(client, syncRoute, inventorySync);
        var secondStockSync = await PostAsync<SyncWriteResult>(client, syncRoute, inventorySync);
        Assert.False(firstStockSync.Replayed);
        Assert.True(secondStockSync.Replayed);
        Assert.Equal(firstStockSync.Result.GetRawText(), secondStockSync.Result.GetRawText());
        var stockAfterSync = await GetAsync<InventoryItemDetails>(client, itemPath);
        Assert.Equal(7m, stockAfterSync.QuantityOnHand);
        Assert.Equal(4, (await GetAsync<List<InventoryTransactionDetails>>(
            client, itemPath + "/transactions")).Count);

        using (var changedPayload = await client.PostAsJsonAsync(syncRoute, new SyncWriteInput
        {
            FarmId = createdFarm.Id, TargetId = item.Id, OperationType = "inventory",
            Movement = new InventoryMovementInput
            {
                TransactionType = StockTransactionType.Receipt,
                QuantityChange = 2m, RowVersion = item.RowVersion
            }
        }))
            Assert.Equal(HttpStatusCode.Conflict, changedPayload.StatusCode);

        var harvestSyncId = Guid.NewGuid();
        var harvestSync = new SyncWriteInput
        {
            FarmId = createdFarm.Id, TargetId = planned.Id, OperationType = "harvest",
            Harvest = new RecordHarvestInput { Quantity = 2m, Notes = "Offline harvest" }
        };
        var harvestSyncRoute = $"/api/sync/operations/{harvestSyncId}";
        Assert.False((await PostAsync<SyncWriteResult>(client, harvestSyncRoute, harvestSync)).Replayed);
        Assert.True((await PostAsync<SyncWriteResult>(client, harvestSyncRoute, harvestSync)).Replayed);
        var finalHarvest = await GetAsync<HarvestOverview>(client, harvests);
        Assert.Equal(3m, finalHarvest.TotalQuantity);
        Assert.Equal(2, finalHarvest.ValidHarvestCount);
        var updatedReport = await GetAsync<HarvestReport>(client, farm + "/reports/harvests");
        Assert.Equal(3m, updatedReport.UnitTotals.Single().Quantity);
        Assert.Equal(1, updatedReport.VoidedHarvestCount);

        var pendingOnCompletion = await PostAsync<FarmTaskDetails>(client, taskPath,
            new FarmTaskInput(4, planting, "Inspect before closure", planned.Id, null));
        var completedCycle = await PostAsync<CropCycleDetails>(client, cycle + "/transitions",
            new CropCycleTransitionInput { RowVersion = active.RowVersion,
                Status = CropCycleStatus.Completed });
        Assert.Equal(CropCycleStatus.Completed, completedCycle.Status);
        Assert.Equal((byte)3, (await GetAsync<List<FarmTaskDetails>>(client, taskPath))
            .Single(t => t.Id == pendingOnCompletion.Id).Status);

        // A second account must not access another owner's farm or harvest.
        using var other = factory.CreateClient();
        var otherEmail = $"phase8-other-{suffix}@example.test";
        await RegisterAsync(other, otherEmail, password);
        await LoginAsync(other, otherEmail, password);
        using var restrictedFarm = await other.GetAsync(farm);
        using var restrictedHarvest = await other.GetAsync(harvests);
        using var restrictedTasks = await other.GetAsync(taskPath);
        using var restrictedWeather = await other.GetAsync(farm + "/weather");
        using var restrictedDashboard = await other.GetAsync(farm + "/dashboard");
        using var restrictedReport = await other.GetAsync(farm + "/reports/harvests");
        using var restrictedSync = await other.PostAsJsonAsync(syncRoute, inventorySync);
        Assert.Equal(HttpStatusCode.NotFound, restrictedSync.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, restrictedFarm.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, restrictedHarvest.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, restrictedTasks.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, restrictedWeather.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, restrictedDashboard.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, restrictedReport.StatusCode);
    }

    private static async Task RegisterAsync(HttpClient client, string email, string password)
    {
        using var response = await client.PostAsJsonAsync("/api/auth/register", new
        {
            FullName = "Phase 8 Tester",
            Email = email,
            Password = password,
            ConfirmPassword = password
        });
        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
    }

    private static async Task LoginAsync(HttpClient client, string email, string password)
    {
        using var response = await client.PostAsJsonAsync("/api/auth/login",
            new { Email = email, Password = password });
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        using var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync());
        var token = json.RootElement.GetProperty("accessToken").GetString();
        Assert.False(string.IsNullOrWhiteSpace(token));
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
    }

    private static async Task<T> PostAsync<T>(HttpClient client, string path, object body)
    {
        using var response = await client.PostAsJsonAsync(path, body);
        Assert.True(response.IsSuccessStatusCode,
            $"{path}: {(int)response.StatusCode} {await response.Content.ReadAsStringAsync()}");
        return (await response.Content.ReadFromJsonAsync<T>())!;
    }

    private static async Task<T> GetAsync<T>(HttpClient client, string path)
    {
        using var response = await client.GetAsync(path);
        Assert.True(response.IsSuccessStatusCode,
            $"{path}: {(int)response.StatusCode} {await response.Content.ReadAsStringAsync()}");
        return (await response.Content.ReadFromJsonAsync<T>())!;
    }
}
