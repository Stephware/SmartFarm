using System.Collections.Concurrent;
using System.Globalization;
using System.Net.Http.Headers;
using System.Text.Json;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

/// <summary>
/// Deliberate, user-submitted location searches only. Public Nominatim does
/// not permit autocomplete. One server instance sends at most one request/s.
/// </summary>
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
[Route("farms/locations")]
public sealed class FarmLocationsController : Controller
{
    private sealed record Place(string Name, decimal Latitude, decimal Longitude);
    private sealed record CachedPlaces(DateTimeOffset Expires, List<Place> Results);
    private sealed record CachedName(DateTimeOffset Expires, string Name);
    private static readonly ConcurrentDictionary<string, CachedName> ReverseCache = new();
    private static readonly ConcurrentDictionary<string, CachedPlaces> Cache =
        new(StringComparer.OrdinalIgnoreCase);
    private static readonly SemaphoreSlim Gate = new(1, 1);
    private static DateTimeOffset _nextRequest = DateTimeOffset.MinValue;
    private static readonly HttpClient Client = BuildClient();

    private static HttpClient BuildClient()
    {
        var client = new HttpClient { Timeout = TimeSpan.FromSeconds(12) };
        client.DefaultRequestHeaders.UserAgent.ParseAdd(
            "AniGoSmartFarm/1.0 (+https://github.com/Stephware/SmartFarm)");
        client.DefaultRequestHeaders.Accept.Add(
            new MediaTypeWithQualityHeaderValue("application/json"));
        return client;
    }

    // Called only after the farmer explicitly selects device GPS, never for each
    // keystroke or marker drag. The normal 1 req/s Nominatim gate is shared with Search.
    [HttpGet("reverse")]
    [ResponseCache(NoStore = true)]
    public async Task<IActionResult> Reverse([FromQuery] decimal? latitude,
        [FromQuery] decimal? longitude, CancellationToken ct)
    {
        if (latitude is null or < -90 or > 90 ||
            longitude is null or < -180 or > 180)
            return BadRequest(new { message = "Select valid latitude and longitude." });

        var lat = decimal.Round(latitude.Value, 4);
        var lon = decimal.Round(longitude.Value, 4);
        var key = lat.ToString("0.0000", CultureInfo.InvariantCulture) + "," +
            lon.ToString("0.0000", CultureInfo.InvariantCulture);
        if (ReverseCache.TryGetValue(key, out var cached) && cached.Expires > DateTimeOffset.UtcNow)
            return Ok(new { name = cached.Name });

        await Gate.WaitAsync(ct);
        try
        {
            if (ReverseCache.TryGetValue(key, out cached) && cached.Expires > DateTimeOffset.UtcNow)
                return Ok(new { name = cached.Name });
            var wait = _nextRequest - DateTimeOffset.UtcNow;
            if (wait > TimeSpan.Zero) await Task.Delay(wait, ct);
            _nextRequest = DateTimeOffset.UtcNow.AddSeconds(1);
            var url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=14&addressdetails=0&lat=" +
                lat.ToString(CultureInfo.InvariantCulture) + "&lon=" +
                lon.ToString(CultureInfo.InvariantCulture);
            using var response = await Client.GetAsync(url, HttpCompletionOption.ResponseHeadersRead, ct);
            if (!response.IsSuccessStatusCode)
                return StatusCode(503, new { message = "Nearby address temporarily unavailable." });
            await using var stream = await response.Content.ReadAsStreamAsync(ct);
            using var json = await JsonDocument.ParseAsync(stream, cancellationToken: ct);
            if (json.RootElement.ValueKind != JsonValueKind.Object ||
                !json.RootElement.TryGetProperty("display_name", out var display) ||
                display.ValueKind != JsonValueKind.String ||
                string.IsNullOrWhiteSpace(display.GetString()))
                return NotFound(new { message = "No nearby address was found." });
            var name = display.GetString()![..Math.Min(display.GetString()!.Length, 250)];
            if (ReverseCache.Count > 500)
                foreach (var oldKey in ReverseCache.Keys.Take(100))
                    ReverseCache.TryRemove(oldKey, out _);
            ReverseCache[key] = new CachedName(DateTimeOffset.UtcNow.AddDays(1), name);
            return Ok(new { name });
        }
        catch (Exception ex) when (ex is HttpRequestException or TaskCanceledException or JsonException)
        {
            return StatusCode(503, new { message = "Nearby address temporarily unavailable." });
        }
        finally { Gate.Release(); }
    }

    [HttpGet("search")]
    [ResponseCache(NoStore = true)]
    public async Task<IActionResult> Search([FromQuery] string? query, CancellationToken ct)
    {
        var term = query?.Trim();
        if (string.IsNullOrWhiteSpace(term) || term.Length is < 3 or > 120)
            return BadRequest(new { message = "Enter 3 to 120 characters to search." });

        // Avoid sending every repeated search to the donation-funded public API.
        var cacheKey = term.ToLowerInvariant();
        if (Cache.TryGetValue(cacheKey, out var cached) && cached.Expires > DateTimeOffset.UtcNow)
            return Ok(cached.Results);

        await Gate.WaitAsync(ct);
        try
        {
            if (Cache.TryGetValue(cacheKey, out cached) && cached.Expires > DateTimeOffset.UtcNow)
                return Ok(cached.Results);

            var wait = _nextRequest - DateTimeOffset.UtcNow;
            if (wait > TimeSpan.Zero) await Task.Delay(wait, ct);
            _nextRequest = DateTimeOffset.UtcNow.AddSeconds(1);

            var url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=5&addressdetails=0&q=" +
                Uri.EscapeDataString(term);
            using var response = await Client.GetAsync(url, HttpCompletionOption.ResponseHeadersRead, ct);
            if (!response.IsSuccessStatusCode)
                return StatusCode(503, new { message = "Location search is temporarily unavailable. You can still place the pin manually." });

            await using var stream = await response.Content.ReadAsStreamAsync(ct);
            using var json = await JsonDocument.ParseAsync(stream, cancellationToken: ct);
            if (json.RootElement.ValueKind != JsonValueKind.Array)
                return StatusCode(503, new { message = "Location search returned an unexpected response." });

            var places = new List<Place>();
            foreach (var item in json.RootElement.EnumerateArray().Take(5))
            {
                if (!item.TryGetProperty("display_name", out var display) ||
                    !item.TryGetProperty("lat", out var latElement) ||
                    !item.TryGetProperty("lon", out var lonElement))
                    continue;
                var name = display.GetString();
                if (string.IsNullOrWhiteSpace(name) ||
                    !decimal.TryParse(latElement.GetString(), NumberStyles.Float,
                        CultureInfo.InvariantCulture, out var latitude) ||
                    !decimal.TryParse(lonElement.GetString(), NumberStyles.Float,
                        CultureInfo.InvariantCulture, out var longitude) ||
                    latitude is < -90 or > 90 || longitude is < -180 or > 180)
                    continue;
                places.Add(new Place(name.Length > 250 ? name[..250] : name,
                    decimal.Round(latitude, 6), decimal.Round(longitude, 6)));
            }

            if (Cache.Count > 500)
                foreach (var key in Cache.Keys.Take(100)) Cache.TryRemove(key, out _);
            Cache[cacheKey] = new CachedPlaces(DateTimeOffset.UtcNow.AddDays(1), places);
            return Ok(places);
        }
        catch (Exception ex) when (ex is HttpRequestException or TaskCanceledException or JsonException)
        {
            return StatusCode(503, new { message = "Location search is temporarily unavailable. Place the pin manually or enter your farm address." });
        }
        finally
        {
            Gate.Release();
        }
    }
}
