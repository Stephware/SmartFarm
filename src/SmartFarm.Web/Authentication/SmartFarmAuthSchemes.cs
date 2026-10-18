namespace SmartFarm.Web.Authentication;

/// <summary>
/// Compile-time aliases for the ASP.NET Core Identity cookie and bearer schemes.
/// Attributes require const values; IdentityConstants fields are static readonly.
/// Keep these values aligned with Microsoft.AspNetCore.Identity.IdentityConstants.
/// </summary>
public static class SmartFarmAuthSchemes
{
    public const string Application = "Identity.Application";
    public const string Bearer = "Identity.Bearer";
}
