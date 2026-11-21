using System.Globalization;
using SmartFarm.Core.Services;
namespace SmartFarm.Web.Presentation;
public static class FarmTimestamp
{
    public static string Display(DateTime utc, string? zone = null, CultureInfo? culture = null) =>
        FarmTime.Local(utc, zone).ToString("MMM d, yyyy · h:mm tt", culture ?? CultureInfo.CurrentCulture);
}
