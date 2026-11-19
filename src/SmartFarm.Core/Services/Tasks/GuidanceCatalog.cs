using SmartFarm.Core.Contracts.Weather;
namespace SmartFarm.Core.Services.Tasks;

// AI composes only reviewed, fact-compatible messages; no generated dosage or emergency claim can enter the output.
public static class GuidanceCatalog
{
    public static string CanonicalCrop(string name) => name.Trim().ToLowerInvariant() switch
    {
        "rice" or "palay" => "rice", "corn" or "mais" => "corn", "pechay" => "pechay",
        "tomato" or "kamatis" => "tomato", "eggplant" or "talong" => "eggplant", "banana" or "saging" => "banana", _ => "other"
    };
    public static string[] Explanations(byte type) => type switch
    { 1 => ["rain.roots"], 2 => ["wind.damage"], 3 => ["heat.stress"], _ => [] };
    public static string[] Recommendations(GuidanceContext context) => context.AdvisoryType switch
    {
        1 => ["drainage.check", "fieldwork.review", "stress.inspect"],
        2 => ["supports.check", "fieldwork.review", "stress.inspect"],
        3 when context.Crop?.Lifecycle == 1 => ["water.check", "fieldwork.review", "stress.inspect"],
        3 => ["fieldwork.review", "stress.inspect"], _ => []
    };
    public static bool IsValid(GuidanceSelection? selection, GuidanceContext context) => selection is not null &&
        selection.ExplanationKeys is { Length: > 0 and <= 2 } && selection.RecommendationKeys is { Length: > 0 and <= 3 } &&
        selection.ExplanationKeys.Distinct().Count() == selection.ExplanationKeys.Length &&
        selection.RecommendationKeys.Distinct().Count() == selection.RecommendationKeys.Length &&
        selection.ExplanationKeys.All(k => Explanations(context.AdvisoryType).Contains(k)) &&
        selection.RecommendationKeys.All(k => Recommendations(context).Contains(k));
    public static GuidanceSelection Fallback(GuidanceContext context) => new(Explanations(context.AdvisoryType), Recommendations(context));
    public static string Text(string key, string? language) => (key, BackendMessages.Language(language)) switch
    {
        ("rain.roots", "fil") => "Maaaring mag-ipon ng tubig sa lupa ang inaasahang malakas na ulan. Maaaring maapektuhan ang mga ugat; suriin muna ang aktuwal na kalagayan.",
        ("rain.roots", _) => "Forecast heavy rain may leave soil waterlogged and affect roots. Check actual field conditions before deciding what to do.",
        ("wind.damage", "fil") => "Maaaring makasira sa pananim at kagamitan ang inaasahang malakas na hangin. Suriin ang panganib bago lumabas.",
        ("wind.damage", _) => "Forecast strong wind may damage crops and equipment. Assess exposure before working outdoors.",
        ("heat.stress", "fil") => "Maaaring madagdagan ng inaasahang init ang stress sa pananim. Hindi nito pinatutunayan na may pinsala na sa bukid.",
        ("heat.stress", _) => "Forecast heat may increase crop stress. It does not establish that damage has occurred on this farm.",
        ("drainage.check", "fil") => "Suriin ang daluyan ng tubig at mga bahaging maaaring bahain kung ligtas itong gawin.",
        ("drainage.check", _) => "Check drainage and areas that may collect water when it is safe to do so.",
        ("supports.check", "fil") => "Suriin ang suporta ng mga pananim at itali nang ligtas ang maluwag na kagamitan bago lumakas ang hangin.",
        ("supports.check", _) => "Check crop supports and secure loose equipment before stronger winds arrive.",
        ("fieldwork.review", "fil") => "Suriin kung dapat ipagpaliban ang gawaing sensitibo sa panahon. Ikaw ang magpapasya sa anumang pagbabago.",
        ("fieldwork.review", _) => "Review whether weather-sensitive work should be postponed. Any changes remain your decision.",
        ("stress.inspect", "fil") => "Inspeksiyunin ang aktuwal na kalagayan ng pananim at itala ang nakikita. Huwag ipalagay na may pinsala batay sa pagtataya lamang.",
        ("stress.inspect", _) => "Inspect actual crop conditions and record what you observe. Do not assume damage from the forecast alone.",
        ("water.check", "fil") => "Suriin muna ang halumigmig ng lupa bago magpasya tungkol sa pagdidilig. Walang takdang dami ng tubig na inirerekomenda rito.",
        ("water.check", _) => "Check soil moisture before deciding about watering. No precise water amount is prescribed here.",
        _ => key
    };
}
