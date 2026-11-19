using SmartFarm.Web.Presentation;
using Xunit;
namespace SmartFarm.Auth.Tests;
public class AniGuideTests
{
    [Theory]
    [InlineData("Owner", false, false, false, "plot")]
    [InlineData("Manager", true, false, false, "plan")]
    [InlineData("Owner", true, true, false, "start")]
    [InlineData("Owner", true, true, true, "record")]
    [InlineData("FieldWorker", false, false, false, null)]
    [InlineData("Unknown", true, false, false, null)]
    public void ChoosesOneAuthorizedActionFromActualState(string role, bool plots, bool planned, bool active, string? expected)
        => Assert.Equal(expected, AniGuide.Step(role, plots, planned, active));
}
