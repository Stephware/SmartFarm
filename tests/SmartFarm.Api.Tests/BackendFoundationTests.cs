using System.Text.Json;
using SmartFarm.Core.Contracts.Query;
using SmartFarm.Core.Services;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class BackendFoundationTests
{
    [Fact]
    public void FarmDateCrossesManilaMidnightWhileInstantStaysUtc()
    {
        var instant = new DateTime(2026, 11, 18, 16, 1, 0, DateTimeKind.Utc);
        Assert.Equal(new DateOnly(2026, 11, 19), FarmTime.Today(instant, "Asia/Manila"));
        Assert.Equal(new DateOnly(2026, 11, 18), FarmTime.Today(instant, "UTC"));
        Assert.Equal(TimeSpan.FromHours(8), FarmTime.Local(instant, "Asia/Manila").Offset);
        Assert.EndsWith("Z\"", JsonSerializer.Serialize(DateTime.SpecifyKind(instant, DateTimeKind.Unspecified), new JsonSerializerOptions { Converters = { new UtcDateTimeConverter() } }));
    }

    [Fact]
    public void InvalidTimeZoneFallsBackExplicitlyAndLanguageDefaultsToEnglish()
    {
        Assert.Equal("Asia/Manila", FarmTime.Resolve("Invalid/Zone").Id);
        Assert.Equal("en", BackendMessages.Language(null));
        Assert.Equal("fil", BackendMessages.Language("fil-PH"));
        Assert.NotEqual(BackendMessages.Text("weather.rain", "en"), BackendMessages.Text("weather.rain", "fil"));
    }

    [Theory]
    [InlineData(0, 25)]
    [InlineData(1, 101)]
    [InlineData(100001, 25)]
    public void PagingRejectsUnboundedOffsetsAndSizes(int page, int size) =>
        Assert.Throws<ArgumentException>(() => new PageRequest { Page = page, PageSize = size }.Validate());

    [Fact]
    public void PagingRejectsReversedDatesUnknownSortAndOversizedSearch()
    {
        Assert.Throws<ArgumentException>(() => new PageRequest { Sort = "sql" }.Validate());
        Assert.Throws<ArgumentException>(() => new PageRequest { Search = new string('x', 121) }.Validate());
        Assert.Throws<ArgumentException>(() => new PageRequest { From = new(2026, 2, 2), To = new(2026, 2, 1) }.Validate());
        Assert.Equal(25, new PageRequest().PageSize);
    }
}
