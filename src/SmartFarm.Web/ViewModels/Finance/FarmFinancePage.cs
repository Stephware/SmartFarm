using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Finance;

namespace SmartFarm.Web.ViewModels.Finance;

public sealed record FarmFinancePage(FarmDetails Farm, FarmFinanceOverview Finance,
    IReadOnlyList<CropCycleDetails> Crops)
{
    public DateOnly FarmToday
    {
        get
        {
            try
            {
                var local = TimeZoneInfo.ConvertTimeFromUtc(DateTime.UtcNow,
                    TimeZoneInfo.FindSystemTimeZoneById(Farm.TimeZoneId));
                return DateOnly.FromDateTime(local);
            }
            catch (Exception error) when (error is TimeZoneNotFoundException or InvalidTimeZoneException)
            {
                return DateOnly.FromDateTime(DateTime.UtcNow);
            }
        }
    }
}
