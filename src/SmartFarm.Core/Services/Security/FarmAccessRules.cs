using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Services.Security;

public static class FarmAccessRules
{
    public static bool Allows(FarmAccessLevel level, FarmAction action) => action switch
    {
        FarmAction.View => level is FarmAccessLevel.Owner or FarmAccessLevel.Manager or FarmAccessLevel.FieldWorker,
        FarmAction.Manage => level is FarmAccessLevel.Owner or FarmAccessLevel.Manager,
        FarmAction.ManageMembers => level == FarmAccessLevel.Owner,
        _ => false
    };
}

