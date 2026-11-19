using System.Threading.Channels;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using SmartFarm.Infrastructure.Identity;
namespace SmartFarm.Infrastructure.Email;

// Bounded, transient delivery handoff keeps public recovery timing independent of account lookup and SMTP.
public sealed class RecoveryEmailQueue(IServiceScopeFactory scopes, TimeProvider clock, ILogger<RecoveryEmailQueue> logger) : BackgroundService
{
    private sealed record Work(string? ResetEmail, SecurityEmail? Confirmation, DateTimeOffset QueuedAt);
    private readonly Channel<Work> queue = Channel.CreateBounded<Work>(new BoundedChannelOptions(100)
    { SingleReader = true, FullMode = BoundedChannelFullMode.Wait });
    public bool TryRequest(string email) => queue.Writer.TryWrite(new(email, null, clock.GetUtcNow()));
    public bool TryConfirmation(SecurityEmail message) => queue.Writer.TryWrite(new(null, message, clock.GetUtcNow()));
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        try
        {
            await foreach (var work in queue.Reader.ReadAllAsync(stoppingToken))
            {
                if (clock.GetUtcNow() - work.QueuedAt > TimeSpan.FromMinutes(2))
                { logger.LogWarning("Security email queue item expired before processing."); continue; }
                try
                {
                    await using var scope = scopes.CreateAsyncScope();
                    var recovery = scope.ServiceProvider.GetRequiredService<AccountRecoveryService>();
                    if (work.ResetEmail is { } email) await recovery.SendRequestedAsync(email, stoppingToken);
                    else if (work.Confirmation is { } message) await recovery.DeliverAsync(message, stoppingToken);
                }
                catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested) { throw; }
                catch (Exception error) { logger.LogWarning("Security email processing failed ({FailureType}).", error.GetType().Name); }
            }
        }
        catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested) { }
    }
}
