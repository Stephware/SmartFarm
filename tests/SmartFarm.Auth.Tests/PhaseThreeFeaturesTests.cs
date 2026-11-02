using System.ComponentModel.DataAnnotations;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Finance;
using SmartFarm.Core.Enums;
using SmartFarm.Web.ViewModels.Farms;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class PhaseThreeFeaturesTests
{
    [Fact]
    public void PlotTilesShowOnlyExistingActiveOrPlannedCrops()
    {
        var farmId = Guid.NewGuid();
        var plotId = Guid.NewGuid();
        var farm = new FarmDetails(farmId, "Demo", "North", "Asia/Manila", null, null, "Owner", "v");
        var plot = new PlotDetails(plotId, farmId, "North field", 100m, "v");
        CropCycleDetails Crop(CropCycleStatus status, string name) =>
            new(Guid.NewGuid(), farmId, plotId, plot.Name, Guid.NewGuid(), name, status,
                new DateOnly(2026, 10, 1), null, null, "kg", null, null, null, "v");
        var model = new PlotLayoutPage(farm, [plot],
            [Crop(CropCycleStatus.Completed, "Past"), Crop(CropCycleStatus.Active, "Tomato")]);
        var tile = Assert.Single(model.Tiles);
        Assert.Equal("Tomato", tile.CropName);
        Assert.Equal(CropCycleStatus.Active, tile.CropStatus);
        Assert.Equal(100m, tile.AreaSqMeters);
        Assert.Null(Assert.Single(new PlotLayoutPage(farm, [plot],
            [Crop(CropCycleStatus.Completed, "Past")]).Tiles).CropName);
    }

    [Fact]
    public void FinanceInputRejectsInvalidAmountsAndTypes()
    {
        var value = new FarmFinanceInput
        {
            Kind = "Expense", Category = "Seeds", AmountPhp = 125.50m,
            EntryDate = new DateOnly(2026, 10, 30)
        };
        Assert.Empty(Validate(value));
        value.AmountPhp = 0;
        Assert.NotEmpty(Validate(value));
        value.AmountPhp = 125.50m;
        value.Kind = "Transfer";
        Assert.NotEmpty(Validate(value));
        value.Kind = "Income";
        Assert.Empty(Validate(value));
    }

    private static IReadOnlyList<ValidationResult> Validate(object model)
    {
        var results = new List<ValidationResult>();
        Validator.TryValidateObject(model, new ValidationContext(model), results, true);
        return results;
    }
}
