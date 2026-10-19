using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Security.Claims;
using System.Text.Json;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.BearerToken;
using Microsoft.AspNetCore.Identity;
using Microsoft.Extensions.Options;
using SmartFarm.Core.Contracts.Farms;

namespace SmartFarm.Web.Clients;

// Server-side MVC requests call the same API used by Android.
public sealed class SmartFarmApiClient(
    HttpClient client,
    IHttpContextAccessor contextAccessor,
    IOptionsMonitor<BearerTokenOptions> bearerOptions)
{
    private HttpRequestMessage NewRequest(HttpMethod method, string path, Guid actor, object? body = null)
    {
        var user = contextAccessor.HttpContext?.User;
        var userId = user?.FindFirst(ClaimTypes.NameIdentifier)?.Value;

        if (user?.Identity?.IsAuthenticated != true ||
            !Guid.TryParse(userId, out var authenticatedId) || actor != authenticatedId)
            throw new FarmOperationException(FarmError.NotFound, "Resource not found.");

        var now = DateTimeOffset.UtcNow;
        var ticket = new AuthenticationTicket(user,
            new AuthenticationProperties { IssuedUtc = now, ExpiresUtc = now.AddMinutes(2) },
            IdentityConstants.BearerScheme);
        var token = bearerOptions.Get(IdentityConstants.BearerScheme)
            .BearerTokenProtector.Protect(ticket);

        var request = new HttpRequestMessage(method, path);
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        if (body is not null) request.Content = JsonContent.Create(body);
        return request;
    }

    private async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken ct)
    {
        try
        {
            var response = await client.SendAsync(request,
                HttpCompletionOption.ResponseHeadersRead, ct);
            if (response.IsSuccessStatusCode) return response;

            string message = "The request could not be completed.";
            if (response.StatusCode is HttpStatusCode.BadRequest or HttpStatusCode.Conflict)
            {
                try
                {
                    await using var data = await response.Content.ReadAsStreamAsync(ct);
                    using var document = await JsonDocument.ParseAsync(data, cancellationToken: ct);
                    if (document.RootElement.ValueKind == JsonValueKind.Object &&
                        document.RootElement.TryGetProperty("message", out var value) &&
                        value.ValueKind == JsonValueKind.String &&
                        value.GetString() is { Length: > 0 and <= 300 } reason)
                        message = reason;
                }
                catch (JsonException) { }
            }

            var status = response.StatusCode;
            response.Dispose();
            throw status switch
            {
                HttpStatusCode.NotFound or HttpStatusCode.Forbidden or HttpStatusCode.Unauthorized =>
                    new FarmOperationException(FarmError.NotFound, "Resource not found."),
                HttpStatusCode.Conflict =>
                    new FarmOperationException(FarmError.Conflict, message),
                HttpStatusCode.BadRequest =>
                    new FarmOperationException(FarmError.InvalidInput, message),
                _ => new HttpRequestException("SmartFarm API is unavailable.", null, status)
            };
        }
        catch (HttpRequestException error) when (error.StatusCode is null)
        {
            throw new HttpRequestException("SmartFarm API is unavailable.", error);
        }
    }

    public async Task<T> ReadAsync<T>(string path, Guid actor, CancellationToken ct)
    {
        using var request = NewRequest(HttpMethod.Get, path, actor);
        using var response = await SendAsync(request, ct);
        return await response.Content.ReadFromJsonAsync<T>(cancellationToken: ct)
            ?? throw new HttpRequestException("SmartFarm API returned an empty response.");
    }

    public async Task<T?> ReadOptionalAsync<T>(string path, Guid actor, CancellationToken ct) where T : class
    {
        try { return await ReadAsync<T>(path, actor, ct); }
        catch (FarmOperationException problem) when (problem.Error == FarmError.NotFound)
        {
            return null;
        }
    }

    public async Task<T> WriteAsync<T>(HttpMethod method, string path, Guid actor,
        object payload, CancellationToken ct)
    {
        using var request = NewRequest(method, path, actor, payload);
        using var response = await SendAsync(request, ct);
        return await response.Content.ReadFromJsonAsync<T>(cancellationToken: ct)
            ?? throw new HttpRequestException("SmartFarm API returned an empty response.");
    }

    public async Task WriteAsync(HttpMethod method, string path, Guid actor,
        object? payload, CancellationToken ct)
    {
        using var request = NewRequest(method, path, actor, payload);
        using var response = await SendAsync(request, ct);
    }

    public async Task<T> UploadAsync<T>(string path, Guid actor, Stream stream,
        long length, string? contentType, CancellationToken ct)
    {
        using var request = NewRequest(HttpMethod.Post, path, actor);
        using var form = new MultipartFormDataContent();
        using var file = new StreamContent(stream);
        file.Headers.ContentType = new MediaTypeHeaderValue(
            contentType is "image/jpeg" or "image/png" or "image/webp"
                ? contentType : "application/octet-stream");
        file.Headers.ContentLength = length;
        form.Add(file, "photo", "inspection-photo");
        request.Content = form;
        using var response = await SendAsync(request, ct);
        return await response.Content.ReadFromJsonAsync<T>(cancellationToken: ct)
            ?? throw new HttpRequestException("SmartFarm API returned an empty response.");
    }

    public async Task<(Stream Stream, string ContentType)> DownloadAsync(
        string path, Guid actor, CancellationToken ct)
    {
        using var request = NewRequest(HttpMethod.Get, path, actor);
        using var response = await SendAsync(request, ct);
        const int maxBytes = 5 * 1024 * 1024;
        if (response.Content.Headers.ContentLength is > maxBytes)
            throw new HttpRequestException("Photo exceeds the supported size.");

        await using var stream = await response.Content.ReadAsStreamAsync(ct);
        using var buffer = new MemoryStream();
        var bytes = new byte[81920];
        int read;
        while ((read = await stream.ReadAsync(bytes, ct)) > 0)
        {
            if (buffer.Length + read > maxBytes)
                throw new HttpRequestException("Photo exceeds the supported size.");
            await buffer.WriteAsync(bytes.AsMemory(0, read), ct);
        }

        var mediaType = response.Content.Headers.ContentType?.MediaType;
        if (mediaType is not ("image/jpeg" or "image/png" or "image/webp"))
            throw new HttpRequestException("API returned an invalid photo type.");
        return (new MemoryStream(buffer.ToArray(), writable: false), mediaType);
    }
}
