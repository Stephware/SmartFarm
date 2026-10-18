using Microsoft.AspNetCore.Identity;
using SmartFarm.Web.Authentication;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class AuthorizationSchemeTests
{
    [Fact]
    public void CompileTimeSchemesMatchIdentityRegistration()
    {
        Assert.Equal(IdentityConstants.ApplicationScheme, SmartFarmAuthSchemes.Application);
        Assert.Equal(IdentityConstants.BearerScheme, SmartFarmAuthSchemes.Bearer);
    }
}
