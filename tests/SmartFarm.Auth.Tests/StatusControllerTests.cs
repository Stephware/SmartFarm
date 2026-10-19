using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Controllers;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class StatusControllerTests
{
    [Fact]
    public void GetReturnsOkWithoutDatabaseOrUser()
    {
        var action = new StatusController().Get();
        var ok = Assert.IsType<OkObjectResult>(action);
        Assert.NotNull(ok.Value);
    }
}
