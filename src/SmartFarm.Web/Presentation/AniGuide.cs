namespace SmartFarm.Web.Presentation;

/// <summary>Presentation only. Records and permissions remain authoritative.</summary>
public static class AniGuide
{
    public static string? Step(string role, bool hasPlots, bool hasPlanned, bool hasActive) =>
        role is not ("Owner" or "Manager") ? null : !hasPlots ? "plot" :
        hasActive ? "record" : hasPlanned ? "start" : "plan";
}
public sealed record AniGuideModel(Guid ActorId, Guid FarmId, string Step);
