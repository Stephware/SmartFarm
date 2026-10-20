using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Weather;
using SmartFarm.Core.Services.Tasks;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Worker.Services;

public sealed class FarmAutomationService(
    SmartFarmDbContext db,
    IWeatherService weather,
    ILogger<FarmAutomationService> logger) : IFarmAutomationService
{
    private static DateOnly LocalDate(Farm farm, DateTime utcNow) =>
        DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(
            utcNow, TimeZoneInfo.FindSystemTimeZoneById(farm.TimeZoneId)));

    public async Task GenerateTasksAndRemindersAsync(CancellationToken ct = default)
    {
        var utcNow = DateTime.UtcNow;
        var farms = await db.Farms.AsNoTracking()
            .Where(f => f.ArchivedAtUtc == null).ToListAsync(ct);

        foreach (var farm in farms)
        {
            DateOnly today;
            try { today = LocalDate(farm, utcNow); }
            catch (TimeZoneNotFoundException)
            {
                logger.LogWarning("Skipping tasks for farm {FarmId}: invalid timezone.", farm.Id);
                continue;
            }

            var horizon = today.AddDays(7);
            var cycles = await db.CropCycles.AsNoTracking()
                .Where(c => c.Status == 0 || c.Status == 1)
                .Where(c => db.Plots.Any(p => p.Id == c.PlotId &&
                    p.FarmId == farm.Id && p.ArchivedAtUtc == null))
                .ToListAsync(ct);
            var keys = (await db.FarmTasks.AsNoTracking()
                    .Where(t => t.FarmId == farm.Id && t.DedupeKey != null)
                    .Select(t => t.DedupeKey!).ToListAsync(ct))
                .ToHashSet(StringComparer.Ordinal);

            foreach (var crop in cycles)
            {
                foreach (var (type, due) in TaskRules.ScheduledTasks(crop, today, horizon))
                {
                    var key = TaskRules.ScheduleKey(crop.Id, type, due);
                    if (!keys.Add(key)) continue;

                    db.FarmTasks.Add(new FarmTask
                    {
                        FarmId = farm.Id,
                        CropCycleId = crop.Id,
                        AssignedToUserId = farm.OwnerUserId,
                        TaskType = type,
                        DueDate = due,
                        Description = type switch
                        {
                            1 => "Planned planting",
                            2 => "Scheduled watering",
                            _ => "Expected harvest"
                        },
                        DedupeKey = key,
                        CreatedAtUtc = utcNow
                    });
                }
            }

            await db.SaveChangesAsync(ct);

            // One in-app reminder for each pending task that is due or overdue.
            var dueTasks = await db.FarmTasks.AsNoTracking()
                .Where(t => t.FarmId == farm.Id && t.Status == 0 && t.DueDate <= today)
                .ToListAsync(ct);
            var activeCropStatuses = cycles.ToDictionary(c => c.Id, c => c.Status);

            foreach (var task in dueTasks)
            {
                byte? cropStatus = task.CropCycleId is { } cycleId &&
                    activeCropStatuses.TryGetValue(cycleId, out var state) ? state : null;
                if (!TaskRules.IsReminderRelevant(task, cropStatus)) continue;

                var recipient = task.AssignedToUserId ?? farm.OwnerUserId;
                var active = await db.Users.AsNoTracking().AnyAsync(
                    u => u.Id == recipient && u.DisabledAtUtc == null, ct);
                var stillMember = recipient == farm.OwnerUserId || await db.FarmMembers.AsNoTracking()
                    .AnyAsync(m => m.FarmId == farm.Id && m.UserId == recipient &&
                        m.RemovedAtUtc == null, ct);
                if (!active || !stillMember)
                {
                    // Removed assignees must not receive farm information.
                    recipient = farm.OwnerUserId;
                    if (!await db.Users.AsNoTracking().AnyAsync(
                        u => u.Id == recipient && u.DisabledAtUtc == null, ct)) continue;
                }

                var key = TaskRules.ReminderKey(task.Id, task.DueDate);
                if (await db.Notifications.AnyAsync(n => n.UserId == recipient &&
                    n.DeduplicationKey == key, ct)) continue;

                db.Notifications.Add(new Notification
                {
                    UserId = recipient,
                    FarmTaskId = task.Id,
                    Type = 1,
                    DeduplicationKey = key,
                    Title = "Farm task due",
                    Message = $"A farm task is due on {task.DueDate:yyyy-MM-dd}.",
                    CreatedAtUtc = utcNow
                });
            }

            await db.SaveChangesAsync(ct);
        }
    }

    public async Task CheckWeatherAsync(CancellationToken ct = default)
    {
        var utcNow = DateTime.UtcNow;
        var farms = await db.Farms.AsNoTracking()
            .Where(f => f.ArchivedAtUtc == null && f.Latitude != null && f.Longitude != null)
            .ToListAsync(ct);

        foreach (var farm in farms)
        {
            DateOnly today;
            try { today = LocalDate(farm, utcNow); }
            catch (Exception error) when (error is TimeZoneNotFoundException or InvalidTimeZoneException)
            {
                logger.LogWarning("Skipping weather for farm {FarmId}: invalid time zone.", farm.Id);
                continue;
            }

            WeatherForecastResult forecast;
            try
            {
                forecast = await weather.GetForecastAsync(
                    new WeatherLocation((double)farm.Latitude!.Value, (double)farm.Longitude!.Value), ct);
            }
            catch (WeatherServiceException error) when (error.Failure == WeatherFailure.NotConfigured)
            {
                logger.LogInformation("Weather checks are disabled until a provider is configured.");
                return;
            }
            catch (WeatherServiceException error)
            {
                logger.LogWarning("Weather unavailable for farm {FarmId}: {Failure}",
                    farm.Id, error.Failure);
                continue;
            }

            var validDays = forecast.Days
                .Where(d => d.Date >= today && d.Date <= today.AddDays(2))
                .Where(WeatherRules.IsPlausible)
                .GroupBy(d => d.Date).Select(g => g.First())
                .OrderBy(d => d.Date).ToList();
            if (validDays.Count == 0)
            {
                logger.LogWarning("No usable forecast days for farm {FarmId}.", farm.Id);
                continue;
            }

            var memberIds = await db.FarmMembers.AsNoTracking()
                .Where(m => m.FarmId == farm.Id && m.RemovedAtUtc == null)
                .Select(m => m.UserId).ToListAsync(ct);
            memberIds.Add(farm.OwnerUserId);
            var recipients = await db.Users.AsNoTracking()
                .Where(u => memberIds.Contains(u.Id) && u.DisabledAtUtc == null)
                .Select(u => u.Id).ToListAsync(ct);

            foreach (var day in validDays)
            {
                // Save the forecast even when no warning is generated.
                var snapshot = new WeatherForecast
                {
                    FarmId = farm.Id,
                    ForecastDate = day.Date,
                    RetrievedAtUtc = utcNow,
                    MinTemperatureC = day.MinimumTemperatureC,
                    MaxTemperatureC = day.MaximumTemperatureC,
                    RainfallMm = day.RainfallMm,
                    MaxPrecipitationProbability = day.MaxPrecipitationProbability,
                    MaxWindKph = day.MaxWindKph,
                    Provider = forecast.Provider.Length > 32 ? forecast.Provider[..32] : forecast.Provider
                };
                db.WeatherForecasts.Add(snapshot);

                var warnings = WeatherRules.Evaluate(day);
                var current = await db.WeatherAdvisories
                    .Where(a => a.FarmId == farm.Id && a.AdvisoryDate == day.Date)
                    .ToListAsync(ct);

                // Automatically resolve a warning when a newer forecast no longer predicts it.
                foreach (var previous in current.Where(a => a.Status == 0 &&
                    warnings.All(w => w.Type != a.AdvisoryType)))
                {
                    previous.Status = 1;
                    previous.ResolvedAtUtc = utcNow;
                }

                var alertRows = new List<WeatherAdvisory>();
                foreach (var warning in warnings)
                {
                    var existing = current.FirstOrDefault(a => a.AdvisoryType == warning.Type);
                    if (existing is null)
                    {
                        existing = new WeatherAdvisory
                        {
                            FarmId = farm.Id, AdvisoryDate = day.Date,
                            AdvisoryType = warning.Type, CreatedAtUtc = utcNow,
                            WeatherForecastId = snapshot.Id
                        };
                        db.WeatherAdvisories.Add(existing);
                    }
                    existing.Severity = warning.Severity;
                    existing.Message = warning.Message;
                    existing.WeatherForecastId = snapshot.Id;
                    existing.Status = 0;
                    existing.ResolvedAtUtc = null;
                    alertRows.Add(existing);
                }

                // Persist the forecast and advisories before creating notification references.
                await db.SaveChangesAsync(ct);
                foreach (var advisory in alertRows)
                {
                    var key = $"weather:{advisory.Id:N}:severity:{advisory.Severity}";
                    foreach (var userId in recipients.Distinct())
                    {
                        if (await db.Notifications.AnyAsync(n => n.UserId == userId &&
                            n.DeduplicationKey == key, ct)) continue;
                        db.Notifications.Add(new Notification
                        {
                            UserId = userId,
                            WeatherAdvisoryId = advisory.Id,
                            Type = 2,
                            DeduplicationKey = key,
                            Title = "Farm weather advisory",
                            Message = advisory.Message,
                            CreatedAtUtc = utcNow
                        });
                    }
                }
                await db.SaveChangesAsync(ct);
            }
        }
    }
}
