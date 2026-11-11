using SmartFarm.Web.Presentation;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class CropArtworkTests
{
    [Theory]
    [InlineData("Tomato", "tomato.svg")]
    [InlineData("Kamatis", "tomato.svg")]
    [InlineData("Japanese Eggplant", "eggplant.svg")]
    [InlineData("Talong", "eggplant.svg")]
    [InlineData("Palay", "rice.svg")]
    [InlineData("Rice", "rice.svg")]
    [InlineData("Sweet corn", "corn.svg")]
    [InlineData("Mais", "corn.svg")]
    [InlineData("Okra", "sprout.svg")]
    [InlineData("", "sprout.svg")]
    [InlineData(null, "sprout.svg")]
    public void CropNamesMapToSafeKnownArtwork(string? cropName, string expected)
    {
        Assert.Equal(expected, CropArtwork.FileName(cropName));
    }
}
