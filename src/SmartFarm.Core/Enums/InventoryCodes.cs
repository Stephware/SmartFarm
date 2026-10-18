namespace SmartFarm.Core.Enums;

public enum InventoryCategory : byte
{
    Seeds = 1,
    Fertilizers = 2,
    Pesticides = 3,
    Other = 4
}

public enum StockTransactionType : byte
{
    Opening = 1,
    Receipt = 2,
    Consumption = 3,
    Adjustment = 4,
    Reversal = 5
}

