namespace SmartFarm.Core.Enums;

// Farm-specific authority, not a global ASP.NET Identity role.
public enum FarmAccessLevel
{
    None = 0,
    FieldWorker = 1,
    Manager = 2,
    Owner = 3
}

public enum FarmAction
{
    View,
    Manage,
    ManageMembers
}

