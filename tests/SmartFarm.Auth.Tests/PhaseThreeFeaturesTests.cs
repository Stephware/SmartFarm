using System.ComponentModel.DataAnnotations;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Finance;
using SmartFarm.Core.Enums;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class PhaseThreeFeaturesTests
{
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
