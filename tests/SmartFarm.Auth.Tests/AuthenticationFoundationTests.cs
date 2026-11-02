using System.ComponentModel.DataAnnotations;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Services.Security;
using SmartFarm.Web.ViewModels.Auth;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class AuthenticationFoundationTests
{
    [Theory]
    [InlineData(FarmAccessLevel.Owner, true, true, true)]
    [InlineData(FarmAccessLevel.Manager, true, true, false)]
    [InlineData(FarmAccessLevel.FieldWorker, true, false, false)]
    [InlineData(FarmAccessLevel.None, false, false, false)]
    public void PermissionMatrixMatchesSqlRoleModel(
        FarmAccessLevel level, bool view, bool manage, bool members)
    {
        Assert.Equal(view, FarmAccessRules.Allows(level, FarmAction.View));
        Assert.Equal(manage, FarmAccessRules.Allows(level, FarmAction.Manage));
        Assert.Equal(members, FarmAccessRules.Allows(level, FarmAction.ManageMembers));
    }

    [Fact]
    public void RegistrationRequiresMatchingPasswordAndValidEmail()
    {
        var invalid = new RegisterInput
        {
            FullName = "Test Farmer", Email = "not-an-email",
            Password = "StrongPassword123", ConfirmPassword = "different"
        };
        var errors = new List<ValidationResult>();
        Assert.False(Validator.TryValidateObject(
            invalid, new ValidationContext(invalid), errors, validateAllProperties: true));
        Assert.NotEmpty(errors);
    }
    [Theory]
    [InlineData(null, true)]
    [InlineData("StrongPassword123", true)]
    [InlineData("different", false)]
    public void RegistrationAcceptsThreeFieldsAndChecksOptionalConfirmation(string? confirmation, bool valid)
    {
        var input = new RegisterInput
        {
            FullName = "Test Farmer", Email = "farmer@example.test", Password = "StrongPassword123",
            ConfirmPassword = confirmation
        };
        Assert.Equal(valid, Validator.TryValidateObject(input, new ValidationContext(input), [], true));
    }

}

