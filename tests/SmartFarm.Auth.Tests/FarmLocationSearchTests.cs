using Microsoft.AspNetCore.Mvc;
using SmartFarm.Web.Controllers;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class FarmLocationSearchTests
{
    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("ab")]
    public async Task SearchRejectsMissingOrShortQuery(string? query)
    {
        var controller = new FarmLocationsController();
        var result = await controller.Search(query, CancellationToken.None);
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task SearchRejectsOversizedQueryWithoutCallingExternalService()
    {
        var controller = new FarmLocationsController();
        var result = await controller.Search(new string('x', 121), CancellationToken.None);
        Assert.IsType<BadRequestObjectResult>(result);
    }
}
