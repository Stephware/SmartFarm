using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Development;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class DemoDataTests
{
    [Fact]
    public void DatasetKeepsStockLifecycleRolesAndPartialHarvestsConsistent()
    {
        var data = DemoDataset.Create(new DateTime(2026, 11, 19, 0, 0, 0, DateTimeKind.Utc));
        var farms = data.OfType<Farm>().ToList();
        Assert.Equal(3, farms.Count);
        Assert.All(farms, f => Assert.Equal("Asia/Manila", f.TimeZoneId));
        Assert.Contains(data.OfType<FarmMember>(), m => m.Role == 1);
        Assert.Contains(data.OfType<FarmMember>(), m => m.Role == 2);
        Assert.All(data.OfType<InventoryItem>(), i => Assert.Equal(i.QuantityOnHand,
            data.OfType<InventoryTransaction>().Where(t => t.InventoryItemId == i.Id).Sum(t => t.QuantityChange)));
        Assert.All(data.OfType<CropCycle>().Where(c => c.Status == 1).GroupBy(c => c.PlotId), g => Assert.Single(g));
        var harvests = data.OfType<Harvest>().ToList();
        Assert.Equal(2, harvests.Count(h => h.Unit == "kg"));
        Assert.Equal(18.75m, harvests.Where(h => h.Unit == "kg").Sum(h => h.Quantity));
        Assert.Contains(data.OfType<WeatherForecast>(), f => f.Provider == "SyntheticDemo");
        Assert.Contains(farms, f => !data.OfType<Plot>().Any(p => p.FarmId == f.Id));
        Assert.Equal(data.Select(Id), DemoDataset.Create(DateTime.UtcNow).Select(Id));
    }

    [Theory]
    [InlineData("Production", "127.0.0.1,14333", "SmartFarmDB", false)]
    [InlineData("Development", "remote.example,1433", "SmartFarmDB", false)]
    [InlineData("Development", "127.0.0.1,14333", "master", false)]
    [InlineData("Development", "127.0.0.1,14333", "SmartFarmDB", true)]
    public void SeedTargetMustBeExplicitLocalDevelopment(string env, string server, string db, bool expected) =>
        Assert.Equal(expected, DemoDataset.IsAllowedTarget(env, server, db));

    private static Guid Id(object entity) => (Guid)entity.GetType().GetProperty("Id")!.GetValue(entity)!;
}
