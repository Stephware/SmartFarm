namespace SmartFarm.Core.Services;

// Stable IDs are presentation contracts. Stored facts/numeric enum codes never change with language.
public static class BackendMessages
{
    public static string Language(string? value) => value?.StartsWith("fil", StringComparison.OrdinalIgnoreCase) == true ? "fil" : "en";
    public static string Text(string key, string? language) => (key, Language(language)) switch
    {
        ("weather.rain", "fil") => "Malakas na ulan ang inaasahan. Suriin ang daluyan ng tubig at planuhin ang gawain sa bukid.",
        ("weather.wind", "fil") => "Malakas na hangin ang inaasahan. Siguraduhing ligtas ang kagamitan bago magtrabaho sa labas.",
        ("weather.heat", "fil") => "Mataas na temperatura ang inaasahan. Suriin ang pananim para sa epekto ng init.",
        ("weather.rain", _) => "Heavy rain is forecast. Check drainage and plan outdoor work.",
        ("weather.wind", _) => "Strong wind is forecast. Secure equipment before outdoor work.",
        ("weather.heat", _) => "High temperature is forecast. Check crops for heat stress.",
        ("weather.caution", "fil") => "Payo batay sa pagtataya, hindi opisyal na babala. Suriin ang pinakabagong abiso ng PAGASA.",
        ("weather.caution", _) => "Forecast-based advice, not an official warning. Check the latest PAGASA notices.",
        ("weather.stale", "fil") => "Luma o kulang ang pagtataya. Kumuha ng bagong impormasyon bago magpasya.",
        ("weather.stale", _) => "The forecast is old or incomplete. Check updated information before deciding.",
        ("task.reminder", "fil") => "May nakatakdang gawain sa bukid. Tingnan ang detalye ng gawain.",
        ("task.reminder", _) => "Farm work is due. Check the task details.",
        ("stock.low", "fil") => "Mababa ang natitirang suplay. Suriin ang imbentaryo.",
        ("stock.low", _) => "Supplies are low. Check inventory.",
        ("enrollment.update", "fil") => "May pagbabago sa iyong kahilingang sumali sa bukid. Tingnan ang katayuan nito.",
        ("enrollment.update", _) => "Your farm enrollment has an update. Check its status.",
        ("recovery.requested", "fil") => "Kung maaaring mabawi ang account, makakatanggap ka ng email na may mga tagubilin.",
        ("recovery.requested", _) => "If the account can be recovered, an email with instructions will be sent.",
        ("recovery.invalid", "fil") => "Hindi magamit ang reset. Humiling ng bagong link o suriin ang password.",
        ("recovery.invalid", _) => "Reset could not be completed. Request a new link or check the password.",
        _ => key
    };
    public static string WeatherKey(byte type) => type switch { 1 => "weather.rain", 2 => "weather.wind", 3 => "weather.heat", _ => "weather.caution" };
}
