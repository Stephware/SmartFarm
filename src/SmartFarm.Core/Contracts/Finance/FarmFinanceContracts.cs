using System.ComponentModel.DataAnnotations;

namespace SmartFarm.Core.Contracts.Finance;

public sealed class FarmFinanceInput
{
    [Required, RegularExpression("Expense|Income")]
    public string Kind { get; set; } = "Expense";
    [Required, StringLength(60, MinimumLength = 2)]
    public string Category { get; set; } = "";
    [Range(typeof(decimal), "0.01", "999999999999.99")]
    public decimal AmountPhp { get; set; }
    public DateOnly EntryDate { get; set; }
    public Guid? CropCycleId { get; set; }
    [StringLength(500)]
    public string? Notes { get; set; }
}

public sealed class FarmFinanceVoidInput
{
    [Required, StringLength(300, MinimumLength = 3)]
    public string Reason { get; set; } = "";
}

public sealed record FarmFinanceEntryDetails(Guid Id, Guid FarmId, Guid? CropCycleId,
    DateOnly EntryDate, string Kind, string Category, decimal AmountPhp, string? Notes,
    DateTime RecordedAtUtc, bool IsVoided, string? VoidReason);

public sealed record FarmFinanceOverview(Guid FarmId, decimal ExpensesPhp,
    decimal IncomePhp, decimal NetPhp, IReadOnlyList<FarmFinanceEntryDetails> Entries);
