using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Services.Inventory;

public static class StockRules
{
    public const decimal MaxQuantity = 999999999999999.999m;

    public static bool IsValidQuantity(decimal value, bool allowZero = false) =>
        value >= 0 && value <= MaxQuantity &&
        decimal.Round(value, 3) == value &&
        (allowZero || value > 0);

    public static bool CanPost(StockTransactionType type, decimal change, Guid? activityId) =>
        change != 0 && decimal.Round(change, 3) == change &&
        change >= -MaxQuantity && change <= MaxQuantity &&
        (type switch
        {
            StockTransactionType.Receipt => change > 0 && activityId is null,
            StockTransactionType.Consumption => change < 0 && activityId is not null,
            StockTransactionType.Adjustment => activityId is null,
            _ => false
        });

    public static bool TryBalance(decimal current, decimal change, out decimal result)
    {
        result = 0;
        try
        {
            result = checked(current + change);
            return IsValidQuantity(result, allowZero: true);
        }
        catch (OverflowException) { return false; }
    }
}

