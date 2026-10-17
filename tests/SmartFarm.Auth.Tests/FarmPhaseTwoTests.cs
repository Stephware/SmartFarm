using System.ComponentModel.DataAnnotations;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Services.Security;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class FarmPhaseTwoTests
{
    [Theory]
    [InlineData(FarmAccessLevel.Owner, true, true)]
    [InlineData(FarmAccessLevel.Manager, true, false)]
    [InlineData(FarmAccessLevel.FieldWorker, false, false)]
    [InlineData(FarmAccessLevel.None, false, false)]
    public void FarmPermissionsRespectMembership(FarmAccessLevel role, bool canManage, bool canInvite)
    {
        Assert.Equal(canManage, FarmAccessRules.Allows(role, FarmAction.Manage));
        Assert.Equal(canInvite, FarmAccessRules.Allows(role, FarmAction.ManageMembers));
    }

    [Fact]
    public void FarmCreationRequiresNameAndLocation()
    {
        var input = new FarmInput { Name = "", LocationText = "" };
        var results = new List<ValidationResult>();
        Assert.False(Validator.TryValidateObject(input, new ValidationContext(input), results, true));
    }

    [Fact]
    public void PlotCreationRejectsInvalidArea()
    {
        var input = new PlotInput { Name = "North", AreaSqMeters = -1 };
        var results = new List<ValidationResult>();
        Assert.False(Validator.TryValidateObject(input, new ValidationContext(input), results, true));
    }

    [Fact]
    public void InvitationsCannotAssignOwnerRole()
    {
        var request = new InviteMemberInput { Email = "test@example.test", Role = 3 };
        var results = new List<ValidationResult>();
        Assert.False(Validator.TryValidateObject(request, new ValidationContext(request), results, true));
    }
}
