using System.Net;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc.Testing;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Enums;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class ReadableCodeContractsTests
{
    private static readonly JsonSerializerOptions WebJson = new(JsonSerializerDefaults.Web);

    [Fact]
    public async Task ReferenceDataListsReadableAllowedCodes()
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient();
        using var response = await client.GetAsync("/api/reference-data");
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);

        using var doc = JsonDocument.Parse(await response.Content.ReadAsStringAsync());
        var root = doc.RootElement;
        Assert.Contains(root.GetProperty("activityTypes").EnumerateArray(),
            x => x.GetProperty("code").GetByte() == 1 &&
                 x.GetProperty("name").GetString() == "Watering");
        Assert.Contains(root.GetProperty("healthStatuses").EnumerateArray(),
            x => x.GetProperty("code").GetByte() == 2 &&
                 x.GetProperty("name").GetString() == "Needs attention");
        Assert.Contains(root.GetProperty("taskCloseStatuses").EnumerateArray(),
            x => x.GetProperty("name").GetString() == "Completed");
        Assert.DoesNotContain(root.GetProperty("taskCloseStatuses").EnumerateArray(),
            x => x.GetProperty("code").GetByte() == 0);
        Assert.DoesNotContain(root.GetProperty("inventoryMovementTypes").EnumerateArray(),
            x => x.GetProperty("name").GetString() == "Opening");
    }

    [Fact]
    public void RequestsAcceptNamesAndLegacyNumbersButKeepNumericOutput()
    {
        var named = JsonSerializer.Deserialize<RecordActivityInput>(
            """{"activityType":"Watering","notes":"Done"}""", WebJson)!;
        var old = JsonSerializer.Deserialize<RecordActivityInput>(
            """{"activityType":1,"notes":"Done"}""", WebJson)!;
        Assert.Equal(old.ActivityType, named.ActivityType);
        Assert.Equal(old.ActivityType, JsonSerializer.Deserialize<RecordActivityInput>(
            """{"activityType":"1"}""", WebJson)!.ActivityType);
        using (var json = JsonDocument.Parse(JsonSerializer.Serialize(named, WebJson)))
            Assert.Equal(1, json.RootElement.GetProperty("activityType").GetByte());

        Assert.Equal((byte)2, JsonSerializer.Deserialize<RecordObservationInput>(
            """{"healthStatus":"Needs attention"}""", WebJson)!.HealthStatus);
        Assert.Equal((byte)5, JsonSerializer.Deserialize<FarmTaskInput>(
            """{"taskType":"Harvest","dueDate":"2026-11-10"}""", WebJson)!.TaskType);
        Assert.Equal((byte)1, JsonSerializer.Deserialize<FarmTaskCloseInput>(
            """{"status":"Completed","rowVersion":"version"}""", WebJson)!.Status);
        Assert.Equal((byte)1, JsonSerializer.Deserialize<UpdateMemberInput>(
            """{"role":"Manager"}""", WebJson)!.Role);
        Assert.Equal(InventoryCategory.Seeds, JsonSerializer.Deserialize<InventoryItemInput>(
            """{"category":"Seeds"}""", WebJson)!.Category);
        Assert.Equal(StockTransactionType.Consumption,
            JsonSerializer.Deserialize<InventoryMovementInput>(
                """{"transactionType":"Consumption"}""", WebJson)!.TransactionType);
        Assert.Equal(StockTransactionType.Consumption,
            JsonSerializer.Deserialize<InventoryMovementInput>(
                """{"transactionType":3}""", WebJson)!.TransactionType);
    }

    [Fact]
    public void UnknownNamedCodesFailInsteadOfSilentlyDefaulting()
    {
        Assert.Throws<JsonException>(() => JsonSerializer.Deserialize<RecordActivityInput>(
            """{"activityType":"Anything"}""", WebJson));
        Assert.Throws<JsonException>(() => JsonSerializer.Deserialize<InventoryItemInput>(
            """{"category":"NotACategory"}""", WebJson));
        Assert.Equal("Unknown", FarmCodeLabels.Activity(255));
        Assert.Equal("Unknown", FarmCodeLabels.Health(255));
        Assert.Equal("Unknown", FarmCodeLabels.TaskStatus(255));
    }

    [Fact]
    public void ResponsesKeepLegacyFieldsAndIncludeReadableNames()
    {
        var activity = new ActivityDetails(Guid.NewGuid(), 2, DateTime.UtcNow, DateTime.UtcNow,
            Guid.NewGuid(), null);
        var observation = new ObservationDetails(Guid.NewGuid(), 3, null,
            DateTime.UtcNow, DateTime.UtcNow, Guid.NewGuid(), null, []);
        var task = new FarmTaskDetails(Guid.NewGuid(), Guid.NewGuid(), null, null, 5,
            new DateOnly(2026, 11, 15), 0, null, DateTime.UtcNow, null, "");
        var alert = new WeatherAdvisoryDetails(Guid.NewGuid(), new DateOnly(2026, 11, 15),
            1, 3, 0, "Heavy rain", DateTime.UtcNow);

        using var activityJson = JsonDocument.Parse(JsonSerializer.Serialize(activity, WebJson));
        Assert.Equal(2, activityJson.RootElement.GetProperty("activityType").GetByte());
        Assert.Equal("Fertilizing", activityJson.RootElement.GetProperty("activityTypeName").GetString());
        using var observationJson = JsonDocument.Parse(JsonSerializer.Serialize(observation, WebJson));
        Assert.Equal("Critical", observationJson.RootElement.GetProperty("healthStatusName").GetString());
        using var taskJson = JsonDocument.Parse(JsonSerializer.Serialize(task, WebJson));
        Assert.Equal("Harvest", taskJson.RootElement.GetProperty("taskTypeName").GetString());
        Assert.Equal("Pending", taskJson.RootElement.GetProperty("statusName").GetString());
        using var weatherJson = JsonDocument.Parse(JsonSerializer.Serialize(alert, WebJson));
        Assert.Equal("Heavy rain", weatherJson.RootElement.GetProperty("typeName").GetString());
        Assert.Equal("Severe", weatherJson.RootElement.GetProperty("severityName").GetString());
    }
}
