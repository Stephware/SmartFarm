using System.Net.Http.Json;
using System.Security.Cryptography;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Text.RegularExpressions;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Services.Tasks;

namespace SmartFarm.Infrastructure.Integrations.Gemini;

public sealed class GeminiOptions
{
    public bool Enabled { get; set; }
    public string? ApiKey { get; set; }
    public string? Model { get; set; }
    public int TimeoutSeconds { get; set; } = 8;
    public int DailyRequestLimit { get; set; } = 20;
    public int MaxOutputTokens { get; set; } = 256;
    public bool Valid() => !Enabled || !string.IsNullOrWhiteSpace(ApiKey) && !ApiKey.Any(char.IsWhiteSpace) &&
        Model is not null && Regex.IsMatch(Model, "^[a-z0-9][a-z0-9.-]{0,79}$", RegexOptions.CultureInvariant) &&
        TimeoutSeconds is >= 1 and <= 30 && DailyRequestLimit is >= 1 and <= 1000 && MaxOutputTokens is >= 64 and <= 512;
}
public sealed class GeminiGuidanceState
{
    internal readonly SemaphoreSlim Gate = new(1, 1);
    internal readonly Dictionary<string, (DateTimeOffset Expires, GuidanceSelection Selection)> Cache = new();
    internal DateOnly Day;
    internal int Requests;
}

public sealed class GeminiGuidanceClient(HttpClient http, IOptions<GeminiOptions> options, GeminiGuidanceState state,
    TimeProvider clock, ILogger<GeminiGuidanceClient> logger) : IWeatherGuidanceProvider
{
    private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web)
    { UnmappedMemberHandling = JsonUnmappedMemberHandling.Disallow, MaxDepth = 16 };
    public async Task<GuidanceSelection?> SelectAsync(GuidanceContext context, CancellationToken ct = default)
    {
        var o = options.Value;
        if (!o.Enabled || !o.Valid()) return null;
        using var timeout = CancellationTokenSource.CreateLinkedTokenSource(ct);
        timeout.CancelAfter(TimeSpan.FromSeconds(o.TimeoutSeconds));
        var held = false;
        try
        {
            await state.Gate.WaitAsync(timeout.Token); held = true;
            var now = clock.GetUtcNow();
            var key = Convert.ToHexString(SHA256.HashData(JsonSerializer.SerializeToUtf8Bytes(new { o.Model, context }, Json)));
            foreach (var old in state.Cache.Where(kv => kv.Value.Expires <= now).Select(kv => kv.Key).ToList()) state.Cache.Remove(old);
            if (state.Cache.TryGetValue(key, out var cached)) return cached.Selection;
            var day = DateOnly.FromDateTime(now.UtcDateTime);
            if (state.Day != day) { state.Day = day; state.Requests = 0; }
            if (state.Requests >= o.DailyRequestLimit) return null;
            state.Requests++;
            var explanations = GuidanceCatalog.Explanations(context.AdvisoryType);
            var recommendations = GuidanceCatalog.Recommendations(context);
            var schema = new
            {
                type = "object", additionalProperties = false,
                properties = new
                {
                    explanationKeys = new { type = "array", items = new { type = "string", @enum = explanations }, minItems = 1, maxItems = 2 },
                    recommendationKeys = new { type = "array", items = new { type = "string", @enum = recommendations }, minItems = 1, maxItems = 3 }
                }, required = new[] { "explanationKeys", "recommendationKeys" }
            };
            using var request = new HttpRequestMessage(HttpMethod.Post,
                $"https://generativelanguage.googleapis.com/v1beta/models/{o.Model}:generateContent");
            request.Headers.Add("x-goog-api-key", o.ApiKey);
            request.Content = JsonContent.Create(new
            {
                systemInstruction = new { parts = new[] { new { text = "Select clear explanatory messages and practical recommendations for these verified forecast facts and optional recorded crop context. Return only permitted catalog keys. Never infer actual field conditions, change severity or generate chemical/application advice. No tools or record changes are available." } } },
                contents = new[] { new { role = "user", parts = new[] { new { text = JsonSerializer.Serialize(context, Json) } } } },
                generationConfig = new { candidateCount = 1, maxOutputTokens = o.MaxOutputTokens, responseMimeType = "application/json", responseJsonSchema = schema }
            });
            using var response = await http.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, timeout.Token);
            if (!response.IsSuccessStatusCode) { logger.LogWarning("Gemini guidance unavailable (HTTP {Status}).", (int)response.StatusCode); return null; }
            await using var stream = await response.Content.ReadAsStreamAsync(timeout.Token);
            using var buffer = new MemoryStream();
            var chunk = new byte[4096]; int read;
            while ((read = await stream.ReadAsync(chunk, timeout.Token)) > 0)
            {
                if (buffer.Length + read > 32768) return null;
                await buffer.WriteAsync(chunk.AsMemory(0, read), timeout.Token);
            }
            using var document = JsonDocument.Parse(buffer.ToArray(), new JsonDocumentOptions { MaxDepth = 16 });
            var candidates = document.RootElement.GetProperty("candidates");
            if (candidates.GetArrayLength() != 1 || candidates[0].GetProperty("finishReason").GetString() != "STOP") return null;
            var parts = candidates[0].GetProperty("content").GetProperty("parts");
            if (parts.GetArrayLength() != 1) return null;
            var selection = JsonSerializer.Deserialize<GuidanceSelection>(parts[0].GetProperty("text").GetString()!, Json);
            if (!GuidanceCatalog.IsValid(selection, context)) return null;
            if (state.Cache.Count >= 256) state.Cache.Remove(state.Cache.Keys.First());
            state.Cache[key] = (now.AddHours(1), selection!);
            return selection;
        }
        catch (OperationCanceledException) when (ct.IsCancellationRequested) { throw; }
        catch (Exception error) when (error is HttpRequestException or IOException or OperationCanceledException or JsonException or
            KeyNotFoundException or InvalidOperationException or ArgumentException)
        {
            logger.LogWarning("Gemini guidance unavailable ({FailureType}).", error.GetType().Name);
            return null;
        }
        finally { if (held) state.Gate.Release(); }
    }
}
