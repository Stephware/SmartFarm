using SmartFarm.Core.Contracts.Finance;

namespace SmartFarm.Core.Services.Finance;

// Each input row is an aggregate from SQL, not a truncated history page.
public sealed record FarmFinanceAggregate(
    int Year, int Month, string Kind, string Category,
    Guid? CycleId, decimal AmountPhp, int Count);

public static class FarmFinanceInsightRules
{
    public static FarmFinanceInsights Build(Guid farmId, DateOnly? from, DateOnly? to,
        IEnumerable<FarmFinanceAggregate> rows,
        IReadOnlyDictionary<Guid, string>? cycleLabels = null)
    {
        var data = rows.ToList();
        var income = data.Where(x => x.Kind == "Income").Sum(x => x.AmountPhp);
        var expenses = data.Where(x => x.Kind == "Expense").Sum(x => x.AmountPhp);
        var categories = data.GroupBy(x => new { x.Kind, x.Category })
            .Select(g => new FarmFinanceCategoryTotal(g.Key.Kind, g.Key.Category,
                g.Sum(x => x.AmountPhp), g.Sum(x => x.Count)))
            .OrderBy(x => x.Kind).ThenByDescending(x => x.AmountPhp)
            .ThenBy(x => x.Category).ToList();
        var months = data.GroupBy(x => new { x.Year, x.Month })
            .Select(g =>
            {
                var inMonth = g.Where(x => x.Kind == "Income").Sum(x => x.AmountPhp);
                var outMonth = g.Where(x => x.Kind == "Expense").Sum(x => x.AmountPhp);
                return new FarmFinanceMonthlyTotal($"{g.Key.Year:0000}-{g.Key.Month:00}",
                    inMonth, outMonth, inMonth - outMonth);
            })
            .OrderByDescending(x => x.Month).ToList();
        var cycles = data.GroupBy(x => x.CycleId).Select(g =>
        {
            var inCycle = g.Where(x => x.Kind == "Income").Sum(x => x.AmountPhp);
            var outCycle = g.Where(x => x.Kind == "Expense").Sum(x => x.AmountPhp);
            var label = g.Key is { } id
                ? (cycleLabels?.GetValueOrDefault(id) ?? "Crop cycle " + id.ToString("N")[..6])
                : "Farm-wide / unassigned";
            return new FarmFinanceCropTotal(g.Key, label,
                inCycle, outCycle, inCycle - outCycle, g.Sum(x => x.Count));
        }).OrderByDescending(x => x.IncomePhp + x.ExpensesPhp)
            .ThenBy(x => x.Label).ToList();
        return new FarmFinanceInsights(farmId, from, to, income, expenses,
            income - expenses, data.Sum(x => x.Count), categories, months, cycles);
    }
}
