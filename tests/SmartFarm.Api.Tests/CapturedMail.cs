using SmartFarm.Infrastructure.Email;
using Xunit;
namespace SmartFarm.Api.Tests;
internal static class CapturedMail
{
    public static async Task<SecurityEmail> WaitAsync(CaptureEmailSender sender, string email, string kind)
    {
        using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(5));
        while (!sender.Messages.Any(m => m.To == email && m.Kind == kind)) await Task.Delay(20, timeout.Token);
        return Assert.Single(sender.Messages, m => m.To == email && m.Kind == kind);
    }
}
