using System.Globalization;
using System.Resources;
namespace SmartFarm.Web.Presentation;
/// <summary>Only application-owned UI copy belongs here; never pass user-entered fields.</summary>
public static class UiMessages
{
    private static readonly ResourceManager Resources = new("SmartFarm.Web.Resources.SharedResource", typeof(SharedResource).Assembly);
    public static string Text(string english) => Resources.GetString(english, CultureInfo.CurrentUICulture) ?? english;
}
