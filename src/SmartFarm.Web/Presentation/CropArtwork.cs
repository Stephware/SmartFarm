namespace SmartFarm.Web.Presentation;

/// <summary>Identifies a crop icon by its existing display name. No status or growth is inferred.</summary>
public static class CropArtwork
{
    public static string FileName(string? cropName)
    {
        var name = cropName?.Trim().ToLowerInvariant() ?? "";
        if (name.Contains("tomato") || name.Contains("kamatis")) return "tomato.svg";
        if (name.Contains("eggplant") || name.Contains("talong") || name.Contains("aubergine")) return "eggplant.svg";
        if (name.Contains("rice") || name.Contains("palay") || name.Contains("paddy")) return "rice.svg";
        if (name.Contains("corn") || name.Contains("maize") || name.Contains("mais")) return "corn.svg";
        return "sprout.svg";
    }
}
