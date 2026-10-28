using System.ComponentModel.DataAnnotations;
using SmartFarm.Api.Models.Auth;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class RegistrationInputTests
{
    [Fact]
    public void ThreeFieldRegistrationDoesNotRequirePasswordConfirmation()
    {
        var input = new RegisterInput
        {
            FullName = "New Farmer", Email = "farmer@example.test", Password = "StrongPassword123"
        };
        var errors = new List<ValidationResult>();
        Assert.True(Validator.TryValidateObject(input, new ValidationContext(input), errors, true),
            string.Join("; ", errors.Select(error => error.ErrorMessage)));
    }

    [Theory]
    [InlineData("StrongPassword123", true)]
    [InlineData("DifferentPassword123", false)]
    [InlineData("", false)]
    public void SuppliedPasswordConfirmationMustMatch(string confirmation, bool valid)
    {
        var input = new RegisterInput
        {
            FullName = "New Farmer", Email = "farmer@example.test", Password = "StrongPassword123",
            ConfirmPassword = confirmation
        };
        Assert.Equal(valid, Validator.TryValidateObject(input, new ValidationContext(input), [], true));
    }
}
