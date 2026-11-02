using System.Globalization;
using System.Security.Claims;
using System.Threading.RateLimiting;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.DependencyInjection;

namespace SmartFarm.Infrastructure.Security;

public static class EnrollmentRateLimiting
{
    public const string Registration = "registration";
    public const string Login = "login";
    public const string Enrollment = "enrollment";

    public static IServiceCollection AddEnrollmentRateLimiting(this IServiceCollection services) =>
        services.AddRateLimiter(options =>
        {
            AddPolicy(Registration, 5, TimeSpan.FromMinutes(10));
            AddPolicy(Login, 20, TimeSpan.FromMinutes(1));
            AddPolicy(Enrollment, 10, TimeSpan.FromMinutes(1), byUser: true);
            options.RejectionStatusCode = StatusCodes.Status429TooManyRequests;
            options.OnRejected = async (context, ct) =>
            {
                var response = context.HttpContext.Response;
                if (context.Lease.TryGetMetadata(MetadataName.RetryAfter, out var retryAfter))
                    response.Headers.RetryAfter = Math.Max(1, (long)Math.Ceiling(retryAfter.TotalSeconds))
                        .ToString(CultureInfo.InvariantCulture);
                await response.WriteAsJsonAsync(new
                {
                    message = "Too many attempts. Please wait and try again."
                }, ct);
            };

            void AddPolicy(string name, int permits, TimeSpan window, bool byUser = false) =>
                options.AddPolicy(name, context =>
                {
                    var user = context.User.Identity?.IsAuthenticated == true
                        ? context.User.FindFirstValue(ClaimTypes.NameIdentifier) : null;
                    var key = byUser && !string.IsNullOrEmpty(user)
                        ? "user:" + user
                        : "ip:" + (context.Connection.RemoteIpAddress?.ToString() ?? "unknown");
                    return RateLimitPartition.GetFixedWindowLimiter(key, _ => new FixedWindowRateLimiterOptions
                    {
                        PermitLimit = permits, Window = window, QueueLimit = 0, AutoReplenishment = true
                    });
                });
        });
}
