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

            foreach (var task in dueTasks)
            {
                var recipient = task.AssignedToUserId ?? farm.OwnerUserId;
                var active = await db.Users.AsNoTracking().AnyAsync(
                    u => u.Id == recipient && u.DisabledAtUtc == null, ct);
                var stillMember = recipient == farm.OwnerUserId || await db.FarmMembers.AsNoTracking()
                    .AnyAsync(m => m.FarmId == farm.Id && m.UserId == recipient &&
                        m.RemovedAtUtc == null, ct);
                if (!active || !stillMember) continue;

                var key = $"task:{task.Id:N}:due";
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
                logger.LogWarning("Weather provider unavailable for farm {FarmId}: {Message}", farm.Id, error.Message);
                continue;
            }

            DateOnly today;
            try { today = LocalDate(farm, utcNow); }
            catch (TimeZoneNotFoundException) { continue; }

            foreach (var day in forecast.Days.Where(d => d.Date >= today && d.Date <= today.AddDays(2)))
            {
                var warnings = WeatherRules.Evaluate(day);
                if (warnings.Count == 0) continue;

                var snapshot = new WeatherForecast
                {
                    FarmId = farm.Id,
                    ForecastDate = day.Date,
                    RetrievedAtUtc = utcNow,
                    MinTemperatureC = day.MinimumTemperatureC,
                    MaxTemperatureC = day.MaximumTemperatureC,
                    RainfallMm = day.RainfallMm,
                    Provider = forecast.Provider.Length > 32 ? forecast.Provider[..32] : forecast.Provider
                };
                db.WeatherForecasts.Add(snapshot);

                var advisories = new List<WeatherAdvisory>();
                var addedNew = false;
                foreach (var warning in warnings)
                {
                    var existing = await db.WeatherAdvisories.SingleOrDefaultAsync(a =>
                        a.FarmId == farm.Id && a.AdvisoryDate == day.Date &&
                        a.AdvisoryType == warning.Type, ct);

                    if (existing is not null)
                    {
                        if (warning.Severity > existing.Severity)
                        {
                            existing.Severity = warning.Severity;
                            existing.Message = warning.Message;
                        }
                        advisories.Add(existing);
                        continue;
                    }

                    var advisory = new WeatherAdvisory
                    {
                        FarmId = farm.Id,
                        WeatherForecastId = snapshot.Id,
                        AdvisoryDate = day.Date,
                        AdvisoryType = warning.Type,
                        Severity = warning.Severity,
                        Status = 0,
                        Message = warning.Message,
                        CreatedAtUtc = utcNow
                    };
                    db.WeatherAdvisories.Add(advisory);
                    advisories.Add(advisory);
                    addedNew = true;
                }

                // A retry must also send any notifications missed after an advisory was saved.
                if (!addedNew)
                    db.WeatherForecasts.Remove(snapshot);
                else
                    await db.SaveChangesAsync(ct);
                var memberIds = await db.FarmMembers.AsNoTracking()
                    .Where(m => m.FarmId == farm.Id && m.RemovedAtUtc == null)
                    .Select(m => m.UserId).ToListAsync(ct);
                memberIds.Add(farm.OwnerUserId);
                var recipients = await db.Users.AsNoTracking()
                    .Where(u => memberIds.Contains(u.Id) && u.DisabledAtUtc == null)
                    .Select(u => u.Id).ToListAsync(ct);

                foreach (var advisory in advisories)
                foreach (var userId in recipients)
                {
                    var key = $"weather:{advisory.Id:N}:severity:{advisory.Severity}";
                    if (await db.Notifications.AnyAsync(n =>
                        n.UserId == userId && n.DeduplicationKey == key, ct)) continue;

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

                await db.SaveChangesAsync(ct);
            }
        }
    }
}
