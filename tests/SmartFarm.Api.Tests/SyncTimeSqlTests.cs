using System.Net.Http.Json;
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Api.Controllers;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Infrastructure.Development;
using SmartFarm.Infrastructure.Persistence;
using Xunit;
namespace SmartFarm.Api.Tests;
public sealed class SyncTimeSqlTests(BackendSqlFixture fixture) : IClassFixture<BackendSqlFixture>
{
    [Fact]
    public async Task NewSyncResultsHaveUtcInstantsAndHistoricalReplayJsonIsPreserved()
    {
        if (fixture.Factory is null) return;
        using var client = await fixture.Client("owner");
        var operation = Guid.NewGuid();
        var route = $"/api/sync/operations/{operation}";
        var input = new SyncWriteInput { FarmId = DemoDataset.Id("farm0"), TargetId = DemoDataset.Id("crop0"), OperationType = "activity",
            Activity = new RecordActivityInput { ActivityType = 5, Notes = "UTC contract fixture" } };
        using var response = await client.PostAsJsonAsync(route, input); response.EnsureSuccessStatusCode();
        var first = await response.Content.ReadFromJsonAsync<JsonElement>();
        Assert.EndsWith("Z", first.GetProperty("result").GetProperty("recordedAtUtc").GetString());
        using var replay = await client.PostAsJsonAsync(route, input); replay.EnsureSuccessStatusCode();
        var second = await replay.Content.ReadFromJsonAsync<JsonElement>();
        Assert.True(second.GetProperty("replayed").GetBoolean());
        Assert.Equal(first.GetProperty("result").GetRawText(), second.GetProperty("result").GetRawText());
        using var scope = fixture.Factory.Services.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<SmartFarmDbContext>();
        var saved = await db.SyncOperations.SingleAsync(o => o.OperationId == operation);
        var oldJson = saved.ResultJson!.Replace("Z\"", "\""); saved.ResultJson = oldJson; await db.SaveChangesAsync();
        using var oldReplay = await client.PostAsJsonAsync(route, input); oldReplay.EnsureSuccessStatusCode();
        var old = await oldReplay.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Equal(oldJson, old.GetProperty("result").GetRawText());
        Assert.Equal(1, await db.CropActivities.CountAsync(a => a.Id == first.GetProperty("result").GetProperty("id").GetGuid()));
    }
}
