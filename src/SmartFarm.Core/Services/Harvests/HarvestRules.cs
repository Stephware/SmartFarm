namespace SmartFarm.Core.Services.Harvests;

public static class HarvestRules
{
    public const decimal MaxQuantity = 999999999999999.999m;

    public static bool IsValidQuantity(decimal quantity) =>
        quantity > 0 && quantity <= MaxQuantity && decimal.Round(quantity, 3) == quantity;

    // The SQL-first design deliberately separates partial harvesting from
    // the crop cycle's explicit completion action.
    public static bool CanHarvest(byte cycleStatus) => cycleStatus == 1;

    public static bool IsValidVoidReason(string? reason) =>
        reason?.Trim().Length is >= 3 and <= 300;
}

