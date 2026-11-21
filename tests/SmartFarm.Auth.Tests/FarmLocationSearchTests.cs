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

    [Theory]
    [InlineData(-91, 120)]
    [InlineData(15, 181)]
    [InlineData(15, -181)]
    [InlineData(91, 120)]
    public async Task ReverseRejectsOutOfRangeCoordinatesWithoutCallingExternalService(
        decimal latitude, decimal longitude)
    {
        var controller = new FarmLocationsController();
        var result = await controller.Reverse(latitude, longitude, CancellationToken.None);
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task ReverseRequiresBothCoordinates()
    {
        var controller = new FarmLocationsController();
        Assert.IsType<BadRequestObjectResult>(
            await controller.Reverse(null, 120m, CancellationToken.None));
        Assert.IsType<BadRequestObjectResult>(
            await controller.Reverse(15m, null, CancellationToken.None));
    }

    [Fact]
    public async Task SearchRejectsOversizedQueryWithoutCallingExternalService()
    {
        var controller = new FarmLocationsController();
        var result = await controller.Search(new string('x', 121), CancellationToken.None);
        Assert.IsType<BadRequestObjectResult>(result);
    }
}
