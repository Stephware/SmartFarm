# PR48 date-limit fix, verification, and merge

> Source: https://github.com/Stephware/SmartFarm/pull/48; local source and observed test artifacts
> Collected: 2026-10-28
> Published: Unknown

## Observed result

PR48 merged into dev at de548ef108767bcadab47745d63e50fb43f3ea64 after testing head 0418bddc09fceb435b0f77a0114f96c72f8804ff. Local dev and origin/dev match. Their tree is 8fc297390d6d59b2e6a8ab4ffd8271b6ddeb72ea, identical to the tested tree. Original PR head was 9a5165d3ca809f00b61eebec508c6dbb54556d6f and base was 53d4b389b1c14a4d09f1a38c502c54a8b83e89f7.

The shared TaskRules.ScheduledTasks watering loop now uses integer day numbers, converting to DateOnly only inside the inclusive valid window. Both MVC agenda and FarmAutomationService call it. Four new regression cases failed before the fix and passed after: future maximum-date planting with intervals 1 and 32767, final watering at DateOnly.MaxValue, and the agenda itself. Independent review found no blockers. No schema, dependency, authorization, protocol or API validation change was made by this fix.

Full backend suite: 317 passed, 0 failed, 0 skipped. Components: API 71 + Auth 145 + SchemaVerification 14 + Weather 37 + Worker 50. Both disposable SQL connection variables were configured; five actual live SQL workflow/enrollment cases executed. Existing xUnit2000 warnings were left unchanged.

Fresh live Web checks: 51 assertions across 155 requests, all passed, including Owner, Manager, Field Worker and outsider agenda/photo access, exact month projections, escaping, invalid input and legacy timezone fallback, and byte-identical shared PNG uploads in both directions. A separate fresh real API/MVC boundary test accepted plantedDate=9999-12-31, watering interval 1, no expected harvest, and rendered the same crop page with HTTP200 both before and after activation, no ArgumentOutOfRangeException and the calendar heading present. The original HTTP500 is resolved.

The unchanged Android files had already passed 18 JVM tests and 36 full emulator tests at the original PR head. Actual AniGo welcome/offline labels and compiled release resources were checked; no Android rerun after the C#-only fix. Ten production photo-path startup checks and two Web UI script tests passed at the original head; those files were unchanged by the fix. Browser visual interaction, physical devices, release installation, durable production mounts and photo migration were not verified. The previously observed Android stale-stock display issue remains unresolved.

Implemented on dev: monthly Web crop agenda with planned planting/recorded planting/watering/expected harvest; terminal crops excluded; farm month navigation and timezone fallback. AniGo display labels on Web and Android; SmartFarm application identifiers/protocol/default API endpoint unchanged. Both hosts use AddSharedObservationPhotoStore. Outside Development it requires an explicitly configured absolute private photo path outside wwwroot and not a filesystem root. The operator must mount the same durable private volume in both hosts; path validation does not provision storage or migrate existing photos.

Only disposable data was used. Task hosts on ports 5180/5181 and the task database were removed; SmartFarmDB still exists. Generated state and key files were removed. Local wiki and AGENTS.md stay ignored and readable, never staged or pushed.

## Observed artifact: backend-summary.json

{
  "head": "0418bddc09fceb435b0f77a0114f96c72f8804ff",
  "projects": [
    {
      "report": "e2e_net10.0_20261028073926.trx",
      "passed": 71,
      "failed": 0
    },
    {
      "report": "e2e_net10.0_20261028073929.trx",
      "passed": 145,
      "failed": 0
    },
    {
      "report": "e2e_net10.0_20261028073930.trx",
      "passed": 14,
      "failed": 0
    },
    {
      "report": "e2e_net10.0_20261028073933.trx",
      "passed": 37,
      "failed": 0
    },
    {
      "report": "e2e_net10.0_20261028073934.trx",
      "passed": 50,
      "failed": 0
    }
  ],
  "sqlTests": [
    {
      "name": "SmartFarm.Api.Tests.FarmEnrollmentSqlTests.JoiningRegistrationAndConcurrentOwnerApprovalUseRealSql_WhenDisposableDatabaseConfigured",
      "outcome": "Passed",
      "duration": "00:00:00.7624851"
    },
    {
      "name": "SmartFarm.Api.Tests.FarmEnrollmentSqlTests.EnrollmentPersistenceFailureRollsBackTheRealIdentityInsert_WhenDisposableDatabaseConfigured",
      "outcome": "Passed",
      "duration": "00:00:00.1099045"
    },
    {
      "name": "SmartFarm.Api.Tests.BusinessWorkflowSqlTests.OwnerCanManageFarmToHarvestWorkflow_WhenTestDatabaseIsConfigured",
      "outcome": "Passed",
      "duration": "00:00:01.9657676"
    },
    {
      "name": "SmartFarm.Api.Tests.FarmEnrollmentSqlTests.ActiveOwnerAndApplicantReadsCannotDeadlockAcrossFarms_WhenDisposableDatabaseConfigured",
      "outcome": "Passed",
      "duration": "00:00:00.8274239"
    },
    {
      "name": "SmartFarm.Api.Tests.FarmEnrollmentSqlTests.FarmCreationRetriesARandomJoinCodeCollision_WhenDisposableDatabaseConfigured",
      "outcome": "Passed",
      "duration": "00:00:00.1899727"
    }
  ],
  "passed": 317,
  "failed": 0
}


## Observed artifact: date-boundary-result.json

{
  "revision": "PR48 with date-limit fix",
  "farmId": "fdf18e20-ed6e-4a3a-ab47-29d6050a6525",
  "cycleId": "a9c82ad9-5236-42b5-8f2c-ff449cdd4779",
  "beforeActivationWebStatus": 200,
  "apiAcceptedActivation": {
    "id": "a9c82ad9-5236-42b5-8f2c-ff449cdd4779",
    "farmId": "fdf18e20-ed6e-4a3a-ab47-29d6050a6525",
    "plotId": "ff29187c-d664-4a48-80df-73a35b1a4e9b",
    "plotName": "Boundary plot",
    "cropTypeId": "e9f6d109-fc96-43ca-9fcf-2d3889727f5c",
    "cropTypeName": "Boundary crop 49124482",
    "status": 1,
    "plannedPlantingDate": "2026-10-28",
    "plantedDate": "9999-12-31",
    "expectedHarvestDate": null,
    "harvestUnit": "kg",
    "wateringIntervalDays": 1,
    "completedAtUtc": null,
    "terminationReason": null,
    "rowVersion": "AAAAAAAAB/Y="
  },
  "afterActivationWebStatus": 200,
  "argumentOutOfRange": false,
  "stackHasCropAgendaBuilder": false,
  "calendarRendered": true,
  "events": [
    {
      "surface": "API",
      "path": "/api/auth/register",
      "status": 201
    },
    {
      "surface": "API",
      "path": "/api/auth/login",
      "status": 200
    },
    {
      "surface": "API",
      "path": "/api/farms",
      "status": 201
    },
    {
      "surface": "API",
      "path": "/api/farms/fdf18e20-ed6e-4a3a-ab47-29d6050a6525/plots",
      "status": 201
    },
    {
      "surface": "API",
      "path": "/api/farms/fdf18e20-ed6e-4a3a-ab47-29d6050a6525/crop-types",
      "status": 201
    },
    {
      "surface": "API",
      "path": "/api/farms/fdf18e20-ed6e-4a3a-ab47-29d6050a6525/plots/ff29187c-d664-4a48-80df-73a35b1a4e9b/crop-cycles",
      "status": 201
    },
    {
      "surface": "API",
      "path": "/api/farms/fdf18e20-ed6e-4a3a-ab47-29d6050a6525/crop-cycles/a9c82ad9-5236-42b5-8f2c-ff449cdd4779/transitions",
      "status": 200
    }
  ]
}


## Observed artifact: merge.json

{
  "pr": 48,
  "url": "https://github.com/Stephware/SmartFarm/pull/48",
  "merged": true,
  "testedHead": "0418bddc09fceb435b0f77a0114f96c72f8804ff",
  "mergeCommit": "de548ef108767bcadab47745d63e50fb43f3ea64",
  "dev": "de548ef108767bcadab47745d63e50fb43f3ea64",
  "originDev": "de548ef108767bcadab47745d63e50fb43f3ea64",
  "testedTree": "8fc297390d6d59b2e6a8ab4ffd8271b6ddeb72ea",
  "mergedTree": "8fc297390d6d59b2e6a8ab4ffd8271b6ddeb72ea"
}


## Observed artifact: cleanup.txt

Disposable DB absent; SmartFarmDB present; task ports 5180/5181 stopped; generated keys/state removed.


## Observed artifact: web-report.md

# PR48 fixed-build focused Web/API runtime verification

**51/51 assertions passed** against fresh disposable MVC5180/API5181 hosts built from PR48 base `9a5165d3ca809f00b61eebec508c6dbb54556d6f` plus root's `TaskRules.ScheduledTasks` integer-day overflow fix. This run used new accounts/database/photo files, with no prior credentials or fixtures. Root separately owns and reports the formerly failing MaxValue-date case and full317-test backend result.

| Coverage | Actual result |
|---|---|
| MVC setup | Fresh Owner, Manager, Field Worker and outsider registered; Owner approves enrollment and promotes Manager |
| Planned/active agenda | Exact November dates and8-row ordering, October planned planting, December7 projected watering entries |
| History/month/privacy | Completed/Failed/Cancelled excluded; month navigation retains farm; all3 roles identical; outsider404 |
| HTML/input | Unusual crop/plot HTML encoded;7 invalid month values fall back;2000-01/2100-12 render200 |
| Legacy timezone | Root updated exactly1 matching synthetic farm row to invalid zone; default/bad month UTC fallback and explicitNovember render200 |
| Cross-host files | Real MVC multipart upload→API download; Field Worker API multipart upload→MVC download; identical70-byte validPNG |
| Photo privacy | All3 roles can read both hosts; nonmember404; anonymousMVC302/API401; outsider upload404 without state change |
| Headers/public paths | `image/png`, `private, no-store`, `nosniff`;20 exact-storage-filename/public-prefix probes404 |

## Evidence and commands

Python standard-library urllib uses independent cookies, real MVC anti-forgery form tokens and API persisted-state reads. A CRC-valid1×1 RGBA PNG is generated, saved, uploaded and compared byte-for-byte across host boundaries. The normal agenda projection checks remained exact after the shared scheduling fix.

```sh
python3 /private/tmp/smartfarm-pr48-fix-20261028/web/pr48_live.py
python3 /private/tmp/smartfarm-pr48-fix-20261028/web/public_paths.py
python3 /private/tmp/smartfarm-pr48-fix-20261028/web/timezone_fallback.py
```

Core28 + public paths20 + legacy timezone3 =51. Run the last command only after root applies the targeted disposableDB timezone fixture. `checks.json` and `assertion-output.txt` contain pass results; `http-log.json` contains endpoints/statuses and binary headers/hashes; `*-agenda*.html` and invalid-zoneHTML contain rendered evidence; `fixture.png`, `web-upload-api-download.png`, and `api-upload-web-download.png` contain exact image bytes. `results.json`/`state.json` contain synthetic identifiers only: generated password removed after testing, anti-forgery values redacted from HTML. No live account credentials or bearer/cookie values retained in artifacts.

Synthetic run `pr48-web-074159-1350`; farm `2a73e9f2-b535-4492-83c7-50d686766eb0`; active cycle `55675ead-6e7d-4fc2-ad47-8c07d7847146`; planned cycle `544293fd-f696-4316-b514-0b2d39b18d22`. Photo IDs `bb6503be-3d56-4766-8ad2-35eeb93675d5` and `6eb82098-eb22-4b54-bfe9-afcdac24df9d`. Shared directory `/private/tmp/smartfarm-pr48-fix-20261028/photos`. PNG SHA256 `4ff6ab670a58c14270e034e2090d9a432caa263a14e0a25785386b0c12f880b5`. 155 HTTP requests; statuses `{'200': 94, '302': 33, '404': 26, '201': 1, '401': 1}`.

No application/tracked/Git/wiki files modified by this agent. All writes limited to synthetic disposable data and temporary verification artifacts. Root can clean the hosts/database after remaining review work finishes.

Not checked here: browserJS/visual behavior, Android or production-mounted storage. Prior CUA permission denial was not retried or bypassed. Root owns edge-case regression and complete backend validation; this report covers focused normal-path regression behavior.


## Repository source: src/SmartFarm.Core/Services/Tasks/TaskRules.cs at 0418bddc09fceb435b0f77a0114f96c72f8804ff

```
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Services.Tasks;

public static class TaskRules
{
    public static bool IsValidType(byte type) => type is >= 1 and <= 6;
    public static bool IsValidCloseStatus(byte status) => status is >= 1 and <= 3;

    public static string? Validate(FarmTaskInput input)
    {
        if (!IsValidType(input.TaskType) || input.DueDate == default)
            return "Choose a task type and due date.";
        if (input.Description?.Trim().Length > 500)
            return "Task description cannot exceed 500 characters.";
        if (input.CropCycleId == Guid.Empty || input.AssignedToUserId == Guid.Empty)
            return "Choose a valid crop and assignee.";
        return null;
    }

    public static byte? ForActivity(byte activityType) => activityType switch
    {
        1 => (byte)2, // Watering
        2 => (byte)3, // Fertilizing
        _ => null
    };

    public static string? ValidateUpdate(FarmTaskUpdateInput input)
    {
        if (input.DueDate == default || input.Description?.Trim().Length > 500 ||
            input.AssignedToUserId == Guid.Empty)
            return "Choose a valid due date, assignee, and description up to 500 characters.";
        return null;
    }

    // Recording actual work may fulfill a generated farm-calendar task.
    // Manual assignments remain limited to their assignee or farm managers.
    public static bool CanFulfillFromRecord(FarmTask task, Guid actor, bool mayManage) =>
        mayManage || task.AssignedToUserId == actor || task.DedupeKey is not null;

    public static bool Close(FarmTask task, byte status, Guid actor, DateTime utcNow)
    {
        if (task.Status != 0 || !IsValidCloseStatus(status)) return false;
        task.Status = status;
        task.ClosedAtUtc = utcNow;
        task.ClosedByUserId = actor;
        return true;
    }

    // Farm dates use their configured time zone, not the worker's UTC calendar day.
    public static DateOnly FarmDate(string timeZoneId, DateTime utcNow) =>
        DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(
            utcNow, TimeZoneInfo.FindSystemTimeZoneById(timeZoneId)));

    // Create upcoming planting, watering and harvest dates from the crop calendar.
    public static IEnumerable<(byte Type, DateOnly Due)> ScheduledTasks(
        CropCycle crop, DateOnly today, DateOnly lastDay)
    {
        if (crop.Status == 0 && crop.PlannedPlantingDate <= lastDay)
            yield return (1, crop.PlannedPlantingDate);

        if (crop.Status != 1) yield break;

        if (crop.PlantedDate is { } planted && crop.WateringIntervalDays is > 0)
        {
            var interval = crop.WateringIntervalDays.Value;
            var days = today.DayNumber - planted.DayNumber;
            var next = Math.Max(1, (days + interval - 1) / interval);
            for (var dueDay = planted.DayNumber + next * interval;
                dueDay <= lastDay.DayNumber; dueDay += interval)
            {
                if (dueDay >= today.DayNumber) yield return (2, DateOnly.FromDayNumber(dueDay));
            }
        }

        if (crop.ExpectedHarvestDate is { } harvest && harvest <= lastDay)
            yield return (5, harvest);
    }

    public static string ReminderKey(Guid taskId, DateOnly due) =>
        $"task:{taskId:N}:due:{due:yyyyMMdd}";

    public static bool IsReminderRelevant(FarmTask task, byte? cropStatus) =>
        task.Status == 0 && (task.CropCycleId is null ||
            (cropStatus is 0 or 1 && !(task.TaskType == 1 && cropStatus == 1)));

    // Deterministic keys keep recurring jobs safe to rerun.
    public static string ScheduleKey(Guid cropId, byte taskType, DateOnly due) =>
        $"cycle:{cropId:N}:type:{taskType}:date:{due:yyyyMMdd}";
}

```

## Repository source: tests/SmartFarm.Auth.Tests/CropAgendaTests.cs at 0418bddc09fceb435b0f77a0114f96c72f8804ff

```
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Enums;
using SmartFarm.Web.ViewModels.Crops;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class CropAgendaTests
{
    private static CropCycleDetails Cycle(CropCycleStatus status, DateOnly planted,
        DateOnly? expected, short? watering = null) =>
        new(Guid.NewGuid(), Guid.NewGuid(), Guid.NewGuid(), "North Plot",
            Guid.NewGuid(), "Tomato", status, new DateOnly(2026, 10, 28),
            status == CropCycleStatus.Planned ? null : planted, expected, "kg",
            watering, null, null, "version");

    [Fact]
    public void AgendaShowsPlannedPlantingAndExpectedHarvestBeforePlanting()
    {
        var cycle = Cycle(CropCycleStatus.Planned, new DateOnly(2026, 11, 2),
            new DateOnly(2026, 11, 28));
        var calendar = CropAgendaBuilder.Build([cycle], new DateOnly(2026, 11, 15));

        Assert.Equal(new DateOnly(2026, 11, 1), calendar.FirstDay);
        Assert.Equal(new DateOnly(2026, 10, 1), calendar.PreviousMonth);
        Assert.Equal(new DateOnly(2026, 12, 1), calendar.NextMonth);
        Assert.Contains(calendar.Days.SelectMany(d => d.Entries),
            e => e.Kind == "Expected harvest" && e.Date == new DateOnly(2026, 11, 28));
        Assert.DoesNotContain(calendar.Days.SelectMany(d => d.Entries),
            e => e.Kind == "Planned planting");
        Assert.Contains(CropAgendaBuilder.Build([cycle], new DateOnly(2026, 10, 1))
            .Days.SelectMany(d => d.Entries),
            e => e.Kind == "Planned planting" && e.Date == new DateOnly(2026, 10, 28));
    }

    [Fact]
    public void AgendaReusesWateringScheduleAndOmitsEndedCrops()
    {
        var active = Cycle(CropCycleStatus.Active, new DateOnly(2026, 11, 1),
            new DateOnly(2026, 11, 21), watering: 5);
        var completed = Cycle(CropCycleStatus.Completed, new DateOnly(2026, 11, 1),
            new DateOnly(2026, 11, 25), watering: 5);

        var calendar = CropAgendaBuilder.Build([active, completed], new DateOnly(2026, 11, 1));
        var events = calendar.Days.SelectMany(d => d.Entries).ToArray();
        Assert.Equal(1, events.Count(e => e.Kind == "Planted"));
        Assert.Contains(events, e => e.Kind == "Watering" &&
            e.Date == new DateOnly(2026, 11, 6));
        Assert.Contains(events, e => e.Kind == "Expected harvest" &&
            e.Date == new DateOnly(2026, 11, 21));
        Assert.DoesNotContain(events, e => e.Date == new DateOnly(2026, 11, 25));
        Assert.Equal(events.Length, calendar.Days.Sum(d => d.Entries.Count));
    }

    [Fact]
    public void AgendaOmitsFutureBoundaryPlantingWithoutOverflowing()
    {
        var cycle = Cycle(CropCycleStatus.Active, DateOnly.MaxValue, null, watering: 1);

        var calendar = CropAgendaBuilder.Build([cycle], new DateOnly(2026, 10, 1));

        Assert.Empty(calendar.Days);
    }

    [Fact]
    public void MonthSelectionUsesFarmTimeZoneAndRejectsBadInput()
    {
        var utc = new DateTime(2026, 10, 31, 17, 0, 0, DateTimeKind.Utc);
        Assert.Equal(new DateOnly(2026, 11, 1),
            CropAgendaBuilder.SelectMonth(null, "Asia/Manila", utc));
        Assert.Equal(new DateOnly(2026, 12, 1),
            CropAgendaBuilder.SelectMonth("2026-12", "Asia/Manila", utc));
        Assert.Equal(new DateOnly(2026, 11, 1),
            CropAgendaBuilder.SelectMonth("not-a-month", "Asia/Manila", utc));
        Assert.Equal(new DateOnly(2026, 10, 1),
            CropAgendaBuilder.SelectMonth(null, "Invalid/FarmZone", utc));
    }
}

```

## Repository source: tests/SmartFarm.Worker.Tests/FarmAutomationTests.cs at 0418bddc09fceb435b0f77a0114f96c72f8804ff

```
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Services.Tasks;
using SmartFarm.Worker.Jobs;
using SmartFarm.Worker.Services;
using Xunit;

namespace SmartFarm.Worker.Tests;

public sealed class FarmAutomationTests
{
    [Theory]
    [InlineData(0)]
    [InlineData(7)]
    public void TaskTypeOutsideDatabaseRangeIsRejected(byte taskType)
    {
        var input = new FarmTaskInput(taskType, new DateOnly(2026, 10, 20), null, null, null);
        Assert.NotNull(TaskRules.Validate(input));
    }

    [Fact]
    public void TasksRequireValidDatesAndLimitDescriptions()
    {
        Assert.NotNull(TaskRules.Validate(new FarmTaskInput(1, default, null, null, null)));
        Assert.NotNull(TaskRules.Validate(new FarmTaskInput(2,
            new DateOnly(2026, 10, 20), new string('x', 501), null, null)));
        Assert.Null(TaskRules.Validate(new FarmTaskInput(2,
            new DateOnly(2026, 10, 20), "Water crops", null, null)));
    }

    [Fact]
    public void ScheduledTaskKeyIsStableAndSpecificToDate()
    {
        var cropId = Guid.NewGuid();
        var date = new DateOnly(2026, 10, 20);
        Assert.Equal(TaskRules.ScheduleKey(cropId, 2, date),
            TaskRules.ScheduleKey(cropId, 2, date));
        Assert.NotEqual(TaskRules.ScheduleKey(cropId, 2, date),
            TaskRules.ScheduleKey(cropId, 2, date.AddDays(1)));
        Assert.NotEqual(TaskRules.ScheduleKey(cropId, 2, date),
            TaskRules.ScheduleKey(cropId, 5, date));
    }

    [Fact]
    public void ActiveCropsGenerateRegularWateringAndHarvestDates()
    {
        var start = new DateOnly(2026, 10, 20);
        var crop = new CropCycle
        {
            Status = 1,
            PlantedDate = start,
            WateringIntervalDays = 2,
            ExpectedHarvestDate = start.AddDays(5)
        };

        var tasks = TaskRules.ScheduledTasks(crop, start, start.AddDays(6)).ToList();

        Assert.Contains(tasks, t => t.Type == 2 && t.Due == start.AddDays(2));
        Assert.Contains(tasks, t => t.Type == 2 && t.Due == start.AddDays(4));
        Assert.Contains(tasks, t => t.Type == 2 && t.Due == start.AddDays(6));
        Assert.Contains(tasks, t => t.Type == 5 && t.Due == start.AddDays(5));
        Assert.DoesNotContain(tasks, t => t.Type == 1);
    }

    [Fact]
    public void WateringAtDateLimitDoesNotOverflowAfterLastDueDate()
    {
        var crop = new CropCycle
        {
            Status = 1,
            PlantedDate = DateOnly.MaxValue.AddDays(-1),
            WateringIntervalDays = 1
        };

        var tasks = TaskRules.ScheduledTasks(crop, DateOnly.MaxValue, DateOnly.MaxValue).ToList();

        Assert.Equal(((byte)2, DateOnly.MaxValue), Assert.Single(tasks));
    }

    [Theory]
    [InlineData(1)]
    [InlineData(32767)]
    public void FuturePlantingAtDateLimitHasNoWateringInCurrentMonth(short interval)
    {
        var crop = new CropCycle
        {
            Status = 1,
            PlantedDate = DateOnly.MaxValue,
            WateringIntervalDays = interval
        };

        Assert.Empty(TaskRules.ScheduledTasks(crop,
            new DateOnly(2026, 10, 1), new DateOnly(2026, 10, 31)));
    }

    [Fact]
    public void PlannedCropsOnlyGeneratePlantingTasks()
    {
        var day = new DateOnly(2026, 10, 20);
        var crop = new CropCycle
        {
            Status = 0,
            PlannedPlantingDate = day.AddDays(3),
            WateringIntervalDays = 2,
            ExpectedHarvestDate = day.AddDays(5)
        };

        var tasks = TaskRules.ScheduledTasks(crop, day, day.AddDays(7)).ToList();

        var planting = Assert.Single(tasks);
        Assert.Equal((byte)1, planting.Type);
        Assert.Equal(day.AddDays(3), planting.Due);
    }

    [Fact]
    public void OverdueCropDeadlinesAreStillGenerated()
    {
        var today = new DateOnly(2026, 10, 20);
        var planned = new CropCycle
        {
            Status = 0,
            PlannedPlantingDate = today.AddDays(-3)
        };
        var active = new CropCycle
        {
            Status = 1,
            PlantedDate = today.AddDays(-10),
            ExpectedHarvestDate = today.AddDays(-1)
        };

        Assert.Contains(TaskRules.ScheduledTasks(planned, today, today.AddDays(7)),
            task => task.Type == 1 && task.Due == today.AddDays(-3));
        Assert.Contains(TaskRules.ScheduledTasks(active, today, today.AddDays(7)),
            task => task.Type == 5 && task.Due == today.AddDays(-1));
    }

    [Theory]
    [InlineData(0, 34, 0)]
    [InlineData(20, 30, 1)]
    [InlineData(50, 40, 2)]
    public void WeatherWarningsUseConfiguredThresholds(int rainfall, int maximum, int count)
    {
        var date = new DateOnly(2026, 10, 20);
        var forecast = new DailyWeatherForecast(date, 20, maximum, rainfall);
        Assert.Equal(count, WeatherRules.Evaluate(forecast).Count);
    }

    [Fact]
    public void BadWeatherReadingsAreRejectedBeforeDatabaseWrites()
    {
        var date = new DateOnly(2026, 10, 20);
        Assert.True(WeatherRules.IsPlausible(new DailyWeatherForecast(date, 18, 32, 4)));
        Assert.True(WeatherRules.IsPlausible(new DailyWeatherForecast(date, null, null, null)));
        Assert.False(WeatherRules.IsPlausible(new DailyWeatherForecast(date, 35, 20, 3)));
        Assert.False(WeatherRules.IsPlausible(new DailyWeatherForecast(date, 20, 28, -1)));
        Assert.False(WeatherRules.IsPlausible(new DailyWeatherForecast(date, -120, 28, 1)));
        Assert.False(WeatherRules.IsPlausible(new DailyWeatherForecast(date, 20, 120, 1)));
    }

    [Fact]
    public void StrongWindCreatesAnAdvisoryAndRainChanceIsValidated()
    {
        var date = new DateOnly(2026, 10, 20);
        var day = new DailyWeatherForecast(date, 23, 32, 5, 80, 60);
        var warning = Assert.Single(WeatherRules.Evaluate(day));
        Assert.Equal((byte)2, warning.Type);
        Assert.Equal((byte)2, warning.Severity);
        Assert.True(WeatherRules.IsPlausible(day));
        Assert.False(WeatherRules.IsPlausible(day with { MaxPrecipitationProbability = 101 }));
        Assert.False(WeatherRules.IsPlausible(day with { MaxWindKph = -1 }));
        Assert.True(WeatherRules.HasSignal(day, 2));
        Assert.False(WeatherRules.HasSignal(day with { MaxWindKph = null }, 2));
        Assert.False(WeatherRules.HasSignal(day with { RainfallMm = null }, 1));
        Assert.Equal((byte)3,
            Assert.Single(WeatherRules.Evaluate(day with { MaxWindKph = 85 })).Severity);
    }

    [Fact]
    public async Task JobsCallTheAutomationService()
    {
        var service = new FakeAutomationService();
        await new TaskReminderJob(service).ExecuteAsync();
        await new WeatherCheckJob(service).ExecuteAsync();

        Assert.Equal(1, service.TasksCalled);
        Assert.Equal(1, service.WeatherCalled);
    }

    private sealed class FakeAutomationService : IFarmAutomationService
    {
        public int TasksCalled { get; private set; }
        public int WeatherCalled { get; private set; }

        public Task GenerateTasksAndRemindersAsync(CancellationToken ct = default)
        {
            TasksCalled++;
            return Task.CompletedTask;
        }

        public Task CheckWeatherAsync(CancellationToken ct = default)
        {
            WeatherCalled++;
            return Task.CompletedTask;
        }
    }
}

```

## Repository source: src/SmartFarm.Web/ViewModels/Crops/CropAgendaBuilder.cs at 0418bddc09fceb435b0f77a0114f96c72f8804ff

```
using System.Globalization;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Services.Tasks;

namespace SmartFarm.Web.ViewModels.Crops;

public sealed record CropAgendaEntry(DateOnly Date, string Kind, string CropName, string PlotName);
public sealed record CropAgendaDay(DateOnly Date, IReadOnlyList<CropAgendaEntry> Entries);
public sealed record CropAgendaMonth(DateOnly FirstDay, DateOnly PreviousMonth,
    DateOnly NextMonth, IReadOnlyList<CropAgendaDay> Days);

public static class CropAgendaBuilder
{
    // Navigation is scoped to reasonable farm-planning dates, with no database changes.
    public static DateOnly SelectMonth(string? month, string timeZoneId, DateTime utcNow)
    {
        DateOnly current;
        try { current = TaskRules.FarmDate(timeZoneId, utcNow); }
        catch (Exception error) when (error is TimeZoneNotFoundException or InvalidTimeZoneException)
        {
            // Legacy farms with invalid time zones should still be able to see crops.
            current = DateOnly.FromDateTime(utcNow);
        }
        var fallback = new DateOnly(current.Year, current.Month, 1);
        if (!DateOnly.TryParseExact(month is null ? "" : month + "-01", "yyyy-MM-dd",
                CultureInfo.InvariantCulture, DateTimeStyles.None, out var selected) ||
            selected.Year is < 2000 or > 2100)
            return fallback;
        return selected;
    }

    public static CropAgendaMonth Build(IReadOnlyList<CropCycleDetails> cycles, DateOnly month)
    {
        var first = new DateOnly(month.Year, month.Month, 1);
        var last = first.AddMonths(1).AddDays(-1);
        var entries = new List<CropAgendaEntry>();

        foreach (var cycle in cycles)
        {
            if (cycle.Status is not (CropCycleStatus.Planned or CropCycleStatus.Active))
                continue;

            var schedule = new CropCycle
            {
                Id = cycle.Id, Status = (byte)cycle.Status,
                PlannedPlantingDate = cycle.PlannedPlantingDate,
                PlantedDate = cycle.PlantedDate,
                ExpectedHarvestDate = cycle.ExpectedHarvestDate,
                WateringIntervalDays = cycle.WateringIntervalDays
            };

            foreach (var (type, due) in TaskRules.ScheduledTasks(schedule, first, last))
            {
                if (due < first || due > last) continue;
                var kind = type switch
                {
                    1 => "Planned planting",
                    2 => "Watering",
                    5 => "Expected harvest",
                    _ => null
                };
                if (kind is not null)
                    entries.Add(new CropAgendaEntry(due, kind, cycle.CropTypeName, cycle.PlotName));
            }

            // Planned crops also have an expected harvest even before planting begins.
            if (cycle.Status == CropCycleStatus.Planned &&
                cycle.ExpectedHarvestDate is { } plannedHarvest &&
                plannedHarvest >= first && plannedHarvest <= last)
                entries.Add(new CropAgendaEntry(plannedHarvest, "Expected harvest",
                    cycle.CropTypeName, cycle.PlotName));

            // Show the recorded planting date for already active crops.
            if (cycle.Status == CropCycleStatus.Active &&
                cycle.PlantedDate is { } planted && planted >= first && planted <= last)
                entries.Add(new CropAgendaEntry(planted, "Planted",
                    cycle.CropTypeName, cycle.PlotName));
        }

        var days = entries.GroupBy(e => e.Date).OrderBy(g => g.Key)
            .Select(g => new CropAgendaDay(g.Key, g.OrderBy(e => e.Kind)
                .ThenBy(e => e.PlotName).ThenBy(e => e.CropName).ToList()))
            .ToList();
        return new CropAgendaMonth(first, first.AddMonths(-1), first.AddMonths(1), days);
    }
}

```

## Repository source: src/SmartFarm.Web/Controllers/CropsController.cs at 0418bddc09fceb435b0f77a0114f96c72f8804ff

```
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.ViewModels.Crops;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class CropsController : Controller
{
    private readonly UserManager<SmartFarmUser> _users;
    private readonly ICropManagementService _crops;
    private readonly IFarmManagementService _farms;

    public CropsController(
        UserManager<SmartFarmUser> users,
        ICropManagementService crops,
        IFarmManagementService farms)
    {
        _users = users;
        _crops = crops;
        _farms = farms;
    }

    private Guid Actor => Guid.TryParse(_users.GetUserId(User), out var id) ? id : Guid.Empty;

    [HttpGet]
    public Task<IActionResult> Index(Guid farmId, CancellationToken ct, string? month = null) =>
        Workspace(farmId, ct, month: month);

    private async Task<IActionResult> Workspace(Guid farmId, CancellationToken ct,
        CropTypeInput? cropType = null, CropCyclePlanInput? plan = null, Guid? plotId = null,
        CropCycleTransitionInput? transition = null, Guid? cycleId = null, string? month = null)
    {
        var farm = await _farms.GetAsync(farmId, Actor, ct);
        if (farm is null) return NotFound();

        var cycles = await _crops.ListCyclesAsync(farmId, Actor, ct);
        return View("Index", new CropWorkspaceViewModel(
            farm, await _farms.ListPlotsAsync(farmId, Actor, ct),
            await _crops.ListCropTypesAsync(farmId, Actor, ct),
            cycles)
        {
            Agenda = CropAgendaBuilder.Build(cycles,
                CropAgendaBuilder.SelectMonth(month, farm.TimeZoneId, DateTime.UtcNow)),
            CropType = cropType ?? new(), Plan = plan ?? new(), PlanPlotId = plotId,
            Transition = transition ?? new(), TransitionCycleId = cycleId
        });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> CreateCropType(Guid farmId,
        [Bind(Prefix = "CropType")] CropTypeInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return await Workspace(farmId, ct, cropType: input);
        }
        try { await _crops.CreateCropTypeAsync(farmId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            ModelState.AddModelError("", e.Message);
            return await Workspace(farmId, ct, cropType: input);
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Plan(Guid farmId, Guid plotId,
        [Bind(Prefix = "Plan")] CropCyclePlanInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return await Workspace(farmId, ct, plan: input, plotId: plotId);
        }
        try { await _crops.PlanAsync(farmId, plotId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            ModelState.AddModelError("", e.Message);
            return await Workspace(farmId, ct, plan: input, plotId: plotId);
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }

    [HttpGet]
    public async Task<IActionResult> EditPlan(Guid farmId, Guid cycleId, CancellationToken ct)
    {
        var farm = await _farms.GetAsync(farmId, Actor, ct);
        if (farm?.Role is not ("Owner" or "Manager")) return NotFound();

        var cycle = await _crops.GetCycleAsync(farmId, cycleId, Actor, ct);
        if (cycle?.Status != CropCycleStatus.Planned) return NotFound();

        return View(new CropPlanEditViewModel(farm, cycle, new CropCyclePlanInput
        {
            CropTypeId = cycle.CropTypeId,
            PlannedPlantingDate = cycle.PlannedPlantingDate,
            ExpectedHarvestDate = cycle.ExpectedHarvestDate,
            HarvestUnit = cycle.HarvestUnit,
            WateringIntervalDays = cycle.WateringIntervalDays,
            RowVersion = cycle.RowVersion
        }, await _crops.ListCropTypesAsync(farmId, Actor, ct)));
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> EditPlan(Guid farmId, Guid cycleId,
        [Bind(Prefix = "Input")] CropCyclePlanInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return await InvalidEditPlan(farmId, cycleId, input, ct);
        }

        try
        {
            await _crops.UpdatePlanAsync(farmId, cycleId, Actor, input, ct);
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            ModelState.AddModelError("", e.Message);
            return await InvalidEditPlan(farmId, cycleId, input, ct);
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }

    private async Task<IActionResult> InvalidEditPlan(Guid farmId, Guid cycleId,
        CropCyclePlanInput input, CancellationToken ct)
    {
        var result = await EditPlan(farmId, cycleId, ct);
        if (result is ViewResult view && view.Model is CropPlanEditViewModel model)
        {
            view.ViewName = "EditPlan";
            view.ViewData.Model = model with { Input = input };
        }
        return result;
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Transition(Guid farmId, Guid cycleId,
        [Bind(Prefix = "Transition")] CropCycleTransitionInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return await Workspace(farmId, ct, transition: input, cycleId: cycleId);
        }
        try { await _crops.TransitionAsync(farmId, cycleId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            ModelState.AddModelError("", e.Message);
            return await Workspace(farmId, ct, transition: input, cycleId: cycleId);
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }
}


```

## Repository source: src/SmartFarm.Web/Views/Crops/Index.cshtml at 0418bddc09fceb435b0f77a0114f96c72f8804ff

```
@using SmartFarm.Core.Enums
@model SmartFarm.Web.ViewModels.Crops.CropWorkspaceViewModel
@{
    ViewData["Title"] = "Crops";
    var canManage = Model.Farm.Role is "Owner" or "Manager";
}
<partial name="_FarmNavigation" model="Model.Farm.Id" />
<section class="panel">
    <p><a asp-controller="Farms" asp-action="Details" asp-route-id="@Model.Farm.Id">← @Model.Farm.Name</a></p>
    <h1>Crop management</h1>
    <p>Plan planting, track active crops, and review crop history.</p>
    @if (TempData["Error"] is string error) { <p class="error" role="alert">@error</p> }
</section>
@if (Model.Agenda is { } agenda)
{
<section class="panel" aria-labelledby="crop-calendar-heading">
    <h2 id="crop-calendar-heading">Crop calendar</h2>
    <p class="hint">Planting, projected watering, and expected harvest dates for @Model.Farm.Name. Dates use @Model.Farm.TimeZoneId; check farm tasks for completion status.</p>
    <nav class="crop-agenda-nav" aria-label="Calendar month">
        <a asp-controller="Crops" asp-action="Index" asp-route-farmId="@Model.Farm.Id"
           asp-route-month="@agenda.PreviousMonth.ToString("yyyy-MM")" aria-label="Previous month">← Previous</a>
        <strong>@agenda.FirstDay.ToString("MMMM yyyy")</strong>
        <a asp-controller="Crops" asp-action="Index" asp-route-farmId="@Model.Farm.Id"
           asp-route-month="@agenda.NextMonth.ToString("yyyy-MM")" aria-label="Next month">Next →</a>
    </nav>
    @if (agenda.Days.Count == 0)
    {
        <p>No planting, watering, or harvest dates scheduled for this month.</p>
    }
    else
    {
        <ol class="crop-agenda-list">
        @foreach (var day in agenda.Days)
        {
            <li>
                <time datetime="@day.Date.ToString("yyyy-MM-dd")">@day.Date.ToString("ddd, MMM d")</time>
                <ul>
                @foreach (var entry in day.Entries)
                {
                    <li><strong>@entry.Kind</strong> · @entry.CropName <span>— @entry.PlotName</span></li>
                }
                </ul>
            </li>
        }
        </ol>
    }
    <p><a asp-controller="Tasks" asp-action="Index" asp-route-farmId="@Model.Farm.Id">View assigned tasks and reminders →</a></p>
</section>
}
<section class="panel">
    <h2>Crop types</h2>
    @if (Model.CropTypes.Count == 0) { <p>No crop types available yet.</p> }
    else
    {
        <ul class="farm-list">
        @foreach (var type in Model.CropTypes)
        {
            <li><strong>@type.Name</strong> · @type.DefaultHarvestUnit
                @if (type.DefaultGrowthDays is not null) { <span>Growth: @type.DefaultGrowthDays days</span> }
            </li>
        }
        </ul>
    }
    @if (canManage)
    {
        <h3>Add a crop type</h3>
        <p class="hint">Crop types are shared across farms. Existing types cannot be renamed here.</p>
        <form asp-action="CreateCropType" asp-route-farmId="@Model.Farm.Id" method="post">
            <label asp-for="CropType.Name">Crop name</label><input asp-for="CropType.Name" maxlength="100" required /><span asp-validation-for="CropType.Name" class="error"></span>
            <label asp-for="CropType.DefaultHarvestUnit">Harvest unit</label><input asp-for="CropType.DefaultHarvestUnit" maxlength="20" required /><span asp-validation-for="CropType.DefaultHarvestUnit" class="error"></span>
            <label asp-for="CropType.DefaultGrowthDays">Typical growth days (optional)</label>
            <input asp-for="CropType.DefaultGrowthDays" type="number" min="1" max="32767" /><span asp-validation-for="CropType.DefaultGrowthDays" class="error"></span>
            <label asp-for="CropType.DefaultWateringIntervalDays">Watering interval (optional)</label>
            <input asp-for="CropType.DefaultWateringIntervalDays" type="number" min="1" max="32767" /><span asp-validation-for="CropType.DefaultWateringIntervalDays" class="error"></span>
            <button type="submit">Add crop type</button>
        </form>
    }
</section>
<section class="panel">
    <h2>Planting plans</h2>
    @if (canManage && Model.Plots.Count > 0 && Model.CropTypes.Count > 0)
    {
        <form asp-action="Plan" asp-route-farmId="@Model.Farm.Id" method="post">
            <label for="plotId">Plot</label>
            <select id="plotId" name="plotId" required>
                @foreach (var plot in Model.Plots)
                {
                    <option value="@plot.Id" selected="@(Model.PlanPlotId == plot.Id)">@plot.Name</option>
                }
            </select>
            <label asp-for="Plan.CropTypeId">Crop</label>
            <select asp-for="Plan.CropTypeId" required>
                @foreach (var type in Model.CropTypes)
                {
                    <option value="@type.Id">@type.Name</option>
                }
            </select>
            <span asp-validation-for="Plan.CropTypeId" class="error"></span>
            <label asp-for="Plan.PlannedPlantingDate">Planned planting</label>@Html.TextBox("Plan.PlannedPlantingDate", Model.Plan.PlannedPlantingDate == default ? null : Model.Plan.PlannedPlantingDate.ToString("yyyy-MM-dd"), new { type = "date", required = "required" })<span asp-validation-for="Plan.PlannedPlantingDate" class="error"></span>
            <label asp-for="Plan.ExpectedHarvestDate">Expected harvest (optional)</label><input asp-for="Plan.ExpectedHarvestDate" type="date" /><span asp-validation-for="Plan.ExpectedHarvestDate" class="error"></span>
            <label asp-for="Plan.HarvestUnit">Harvest unit (leave empty to use crop default)</label>
            <input asp-for="Plan.HarvestUnit" maxlength="20" /><span asp-validation-for="Plan.HarvestUnit" class="error"></span>
            <label asp-for="Plan.WateringIntervalDays">Watering interval days (leave empty for default)</label>
            <input asp-for="Plan.WateringIntervalDays" type="number" min="1" max="32767" /><span asp-validation-for="Plan.WateringIntervalDays" class="error"></span>
            <button type="submit">Create planting plan</button>
        </form>
    }
    else if (Model.Plots.Count == 0) { <p>Add an active plot before planning a crop.</p> }
    else if (Model.CropTypes.Count == 0) { <p>Add a crop type before planning a crop.</p> }
    else { <p>You have read-only access to planting plans.</p> }
</section>
<section class="panel">
    <h2>Crop cycles and history</h2>
    @if (Model.Cycles.Count == 0) { <p>No crop cycles have been planned yet.</p> }
    @foreach (var cycle in Model.Cycles)
    {
        <div class="crop-cycle">
            <h3>@cycle.CropTypeName · @cycle.PlotName</h3>
            <p><a asp-controller="Harvests" asp-action="Index"
                  asp-route-farmId="@Model.Farm.Id" asp-route-cycleId="@cycle.Id">Harvest records →</a></p>
            <p><a asp-controller="Monitoring" asp-action="Index"
                  asp-route-farmId="@Model.Farm.Id" asp-route-cycleId="@cycle.Id">Activities, inspections and photos →</a></p>
            <p><strong>@cycle.Status</strong> · Planned: @cycle.PlannedPlantingDate.ToString("yyyy-MM-dd")
                @if (cycle.PlantedDate is not null) { <span> · Planted: @cycle.PlantedDate.Value.ToString("yyyy-MM-dd")</span> }
            </p>
            @if (cycle.ExpectedHarvestDate is not null)
            { <p>Expected harvest: @cycle.ExpectedHarvestDate.Value.ToString("yyyy-MM-dd")</p> }
            @if (cycle.TerminationReason is not null)
            { <p>Reason: @cycle.TerminationReason</p> }
            @if (canManage && cycle.Status == CropCycleStatus.Planned)
            {
                <a asp-action="EditPlan" asp-route-farmId="@Model.Farm.Id" asp-route-cycleId="@cycle.Id">Edit plan</a>
                <form asp-action="Transition" asp-route-farmId="@Model.Farm.Id"
                      asp-route-cycleId="@cycle.Id" method="post" data-confirm="Mark as planted: @cycle.CropTypeName on @cycle.PlotName? Review the dates and reason before continuing.">
                    <input type="hidden" name="Transition.RowVersion" value="@cycle.RowVersion" />
                    <input type="hidden" name="Transition.Status" value="Active" />
                    <label for="Transition_PlantedDate_@cycle.Id">Actual planting date</label><input id="Transition_PlantedDate_@cycle.Id" type="date" name="Transition.PlantedDate" value="@(Model.TransitionCycleId == cycle.Id ? Model.Transition.PlantedDate?.ToString("yyyy-MM-dd") : null)" required />
                    <button type="submit">Mark as planted</button>
                </form>
            }
            @if (canManage && cycle.Status == CropCycleStatus.Active)
            {
                <form asp-action="Transition" asp-route-farmId="@Model.Farm.Id"
                      asp-route-cycleId="@cycle.Id" method="post" data-confirm="Complete: @cycle.CropTypeName on @cycle.PlotName? Review the dates and reason before continuing.">
                    <input type="hidden" name="Transition.RowVersion" value="@cycle.RowVersion" />
                    <input type="hidden" name="Transition.Status" value="Completed" />
                    <button type="submit">Complete crop cycle</button>
                </form>
            }
            @if (canManage && cycle.Status is CropCycleStatus.Active or CropCycleStatus.Planned)
            {
                <form asp-action="Transition" asp-route-farmId="@Model.Farm.Id"
                      asp-route-cycleId="@cycle.Id" method="post" data-confirm="Cancel: @cycle.CropTypeName on @cycle.PlotName? Review the dates and reason before continuing.">
                    <input type="hidden" name="Transition.RowVersion" value="@cycle.RowVersion" />
                    <input type="hidden" name="Transition.Status" value="Cancelled" />
                    <label for="Transition_TerminationReason_@cycle.Id">Cancellation reason</label><input id="Transition_TerminationReason_@cycle.Id" name="Transition.TerminationReason" value="@(Model.TransitionCycleId == cycle.Id && Model.Transition.Status == CropCycleStatus.Cancelled ? Model.Transition.TerminationReason : null)" maxlength="300" required />
                    <button class="button-danger" type="submit">Cancel cycle</button>
                </form>
            }
            @if (canManage && cycle.Status == CropCycleStatus.Active)
            {
                <form asp-action="Transition" asp-route-farmId="@Model.Farm.Id"
                      asp-route-cycleId="@cycle.Id" method="post" data-confirm="Mark failed: @cycle.CropTypeName on @cycle.PlotName? Review the dates and reason before continuing.">
                    <input type="hidden" name="Transition.RowVersion" value="@cycle.RowVersion" />
                    <input type="hidden" name="Transition.Status" value="Failed" />
                    <label for="Transition_FailureReason_@cycle.Id">Failure reason</label><input id="Transition_FailureReason_@cycle.Id" name="Transition.TerminationReason" value="@(Model.TransitionCycleId == cycle.Id && Model.Transition.Status == CropCycleStatus.Failed ? Model.Transition.TerminationReason : null)" maxlength="300" required />
                    <button class="button-danger" type="submit">Mark failed</button>
                </form>
            }
        </div>
    }
</section>




```

## Repository source: src/SmartFarm.Infrastructure/Services/Monitoring/ObservationPhotoStorageRegistration.cs at 0418bddc09fceb435b0f77a0114f96c72f8804ff

```
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Core.Interfaces.Monitoring;

namespace SmartFarm.Infrastructure.Services.Monitoring;

public static class ObservationPhotoStorageRegistration
{
    // Both hosts must use the same mounted private directory in deployment.
    // Development keeps the existing shared Web/App_Data location.
    public static IServiceCollection AddSharedObservationPhotoStore(
        this IServiceCollection services, IConfiguration configuration,
        bool isDevelopment, string developmentDefault)
    {
        var root = ResolvePath(configuration["ObservationPhotos:StoragePath"],
            isDevelopment, developmentDefault);
        services.AddSingleton<IObservationPhotoStore>(new LocalObservationPhotoStore(root));
        return services;
    }

    public static string ResolvePath(string? configured, bool isDevelopment, string developmentDefault)
    {
        if (string.IsNullOrWhiteSpace(configured) && !isDevelopment)
            throw new InvalidOperationException(
                "Set ObservationPhotos:StoragePath to the same absolute, mounted private directory " +
                "on both MVC and API hosts before deployment.");

        var value = string.IsNullOrWhiteSpace(configured) ? developmentDefault : configured.Trim();
        if (!Path.IsPathFullyQualified(value))
            throw new InvalidOperationException("ObservationPhotos:StoragePath must be an absolute path.");

        var fullPath = Path.GetFullPath(value);
        if (string.Equals(fullPath, Path.GetPathRoot(fullPath), StringComparison.OrdinalIgnoreCase) ||
            fullPath.Split(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar)
                .Any(part => part.Equals("wwwroot", StringComparison.OrdinalIgnoreCase)))
            throw new InvalidOperationException(
                "Photo storage must be a private directory outside wwwroot, not a drive root.");

        return fullPath;
    }
}

```

## Repository source: src/SmartFarm.Web/Program.cs at 0418bddc09fceb435b0f77a0114f96c72f8804ff

```
using Microsoft.AspNetCore.DataProtection;
using Microsoft.AspNetCore.Diagnostics.HealthChecks;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Infrastructure;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.HealthChecks;
using SmartFarm.Infrastructure.Services.Monitoring;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllersWithViews(options => options.Filters.AddService<SmartFarm.Web.Filters.ApiUnavailableFilter>());
builder.Services.AddScoped<SmartFarm.Web.Filters.ApiUnavailableFilter>();
builder.Services.AddSmartFarmPersistence(
    builder.Configuration.GetConnectionString("SmartFarm"));
builder.Services.AddSmartFarmWebIdentity(builder.Environment);
builder.Services.AddScoped<AccountRegistrationService>();

// MVC and API run independently using the same Infrastructure business services.
builder.Services.AddSmartFarmBusinessServices();
// In Development this matches the API's sibling SmartFarm.Web/App_Data photo store.
// Configure ObservationPhotos:StoragePath to the same shared directory on each host
// before deploying the MVC and API to separate instances.
builder.Services.AddSharedObservationPhotoStore(
    builder.Configuration, builder.Environment.IsDevelopment(),
    Path.GetFullPath(Path.Combine(builder.Environment.ContentRootPath,
        "App_Data", "observation-photos")));
// Web and API must use the same protected key ring when hosted on different machines.
var dataProtection = builder.Services.AddDataProtection()
    .SetApplicationName($"SmartFarm.SharedIdentity.{builder.Environment.EnvironmentName}");
if (builder.Configuration["DataProtection:KeyRingPath"] is { Length: > 0 } keyRingPath)
{
    if (!Path.IsPathFullyQualified(keyRingPath))
        throw new InvalidOperationException("DataProtection:KeyRingPath must be an absolute path.");
    dataProtection.PersistKeysToFileSystem(new DirectoryInfo(keyRingPath));
}

if (builder.Environment.IsDevelopment())
{
    builder.Services.AddHealthChecks()
        .AddCheck<DatabaseConnectivityHealthCheck>("database", timeout: TimeSpan.FromSeconds(5));
}

var app = builder.Build();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler(handler =>
    {
        handler.Run(async context =>
        {
            context.Response.StatusCode = StatusCodes.Status500InternalServerError;
            context.Response.ContentType = "text/plain";
            await context.Response.WriteAsync("An unexpected error occurred.");
        });
    });
    app.UseHsts();
    app.UseHttpsRedirection();
}

app.UseStaticFiles();
app.UseRouting();
app.UseAuthentication();
app.UseAuthorization();

if (app.Environment.IsDevelopment())
{
    app.MapHealthChecks("/health/database", new HealthCheckOptions
    {
        Predicate = check => check.Name == "database"
    });
}

app.MapControllers();
app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}");

app.Run();

public partial class Program { }

```

## Repository source: src/SmartFarm.Api/Program.cs at 0418bddc09fceb435b0f77a0114f96c72f8804ff

```
using System.Data.Common;
using Microsoft.AspNetCore.DataProtection;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Infrastructure;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Integrations.Weather;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Infrastructure.Security;
using SmartFarm.Infrastructure.Services.Crops;
using SmartFarm.Infrastructure.Services.Farms;
using SmartFarm.Infrastructure.Services.Harvests;
using SmartFarm.Infrastructure.Services.Inventory;
using SmartFarm.Infrastructure.Services.Monitoring;
using SmartFarm.Infrastructure.Services.Reports;
using SmartFarm.Infrastructure.Services.Tasks;

var builder = WebApplication.CreateBuilder(args);

// In local Development, Web and API share SmartFarmDB. If only Web's user
// secrets were configured, reuse just its database connection (no other secrets).
// An explicit API connection setting always wins. Production is unaffected.
if (builder.Environment.IsDevelopment() &&
    string.IsNullOrWhiteSpace(builder.Configuration.GetConnectionString("SmartFarm")))
{
    var webDatabase = new ConfigurationBuilder()
        .AddUserSecrets("SmartFarm.Web.Development")
        .Build()
        .GetConnectionString("SmartFarm");
    if (!string.IsNullOrWhiteSpace(webDatabase))
    {
        builder.Configuration.AddInMemoryCollection(new Dictionary<string, string?>
        {
            ["ConnectionStrings:SmartFarm"] = webDatabase
        });
    }
}

builder.Services.AddControllers();
builder.Services.AddSmartFarmPersistence(builder.Configuration.GetConnectionString("SmartFarm"));
builder.Services.AddSmartFarmBusinessServices();
builder.Services.AddSmartFarmWeather(builder.Configuration);
builder.Services.AddSmartFarmApiIdentity();
builder.Services.AddScoped<AccountRegistrationService>();
builder.Services.AddSharedObservationPhotoStore(
    builder.Configuration, builder.Environment.IsDevelopment(),
    Path.GetFullPath(Path.Combine(builder.Environment.ContentRootPath, "..",
        "SmartFarm.Web", "App_Data", "observation-photos")));
// Web and API must use the same protected key ring when hosted on different machines.
var dataProtection = builder.Services.AddDataProtection()
    .SetApplicationName($"SmartFarm.SharedIdentity.{builder.Environment.EnvironmentName}");
if (builder.Configuration["DataProtection:KeyRingPath"] is { Length: > 0 } keyRingPath)
{
    if (!Path.IsPathFullyQualified(keyRingPath))
        throw new InvalidOperationException("DataProtection:KeyRingPath must be an absolute path.");
    dataProtection.PersistKeysToFileSystem(new DirectoryInfo(keyRingPath));
}

var app = builder.Build();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler(handler =>
    {
        handler.Run(async context =>
        {
            context.Response.StatusCode = StatusCodes.Status500InternalServerError;
            context.Response.ContentType = "text/plain";
            await context.Response.WriteAsync("An unexpected error occurred.");
        });
    });
    app.UseHsts();
    app.UseHttpsRedirection();
}

// Return a predictable response for database failures without exposing SQL details.
// The full provider error remains in the API terminal logs.
app.Use(async (context, next) =>
{
    try { await next(); }
    catch (DbException error) when (!context.Response.HasStarted)
    {
        context.RequestServices.GetRequiredService<ILoggerFactory>()
            .CreateLogger("SmartFarm.Api.Database")
            .LogError(error, "Database failure processing {Method} {Path}",
                context.Request.Method, context.Request.Path);
        context.Response.Clear();
        context.Response.StatusCode = StatusCodes.Status503ServiceUnavailable;
        await context.Response.WriteAsJsonAsync(new
        {
            status = "unhealthy",
            message = "SmartFarm database request failed. Check the API terminal and /health/database."
        });
    }
});

app.UseRouting();
app.UseAuthentication();
app.UseAuthorization();

if (app.Environment.IsDevelopment())
{
    app.MapGet("/health/database", async (SmartFarmDbContext db,
        ILogger<Program> logger, CancellationToken ct) =>
    {
        static IResult Unhealthy(string message) =>
            Results.Json(new { status = "unhealthy", message },
                statusCode: StatusCodes.Status503ServiceUnavailable);

        try
        {
            if (string.IsNullOrWhiteSpace(db.Database.GetConnectionString()))
                return Unhealthy("Set ConnectionStrings:SmartFarm for SmartFarmDB.");

            if (!await db.Database.CanConnectAsync(ct))
                return Unhealthy("Cannot connect to SQL Server. Check the connection string and server.");

            // Connecting isn't enough: an empty or wrong database also opens successfully.
            // Probe the tables needed by authentication and the My Farms screen, read-only.
            await db.Users.AsNoTracking().Select(u => u.Id).Take(1).ToListAsync(ct);
            await db.Farms.AsNoTracking().Select(f => f.Id).Take(1).ToListAsync(ct);
            await db.FarmMembers.AsNoTracking().Select(m => m.Id).Take(1).ToListAsync(ct);

            return Results.Ok(new { status = "ok", message = "Database and main farm tables are ready." });
        }
        catch (Microsoft.Data.SqlClient.SqlException error) when (error.Number is 208 or 207)
        {
            logger.LogError(error, "Required SmartFarm schema is missing or incompatible");
            return Unhealthy("SmartFarmDB is missing required farm or identity tables. " +
                "Verify the schema; do not rerun initialization on an existing database.");
        }
        catch (Exception error)
        {
            logger.LogError(error, "SmartFarm database health check failed");
            return Unhealthy("Database check failed. Read the SmartFarm.Api terminal for the reason.");
        }
    });
}

app.MapControllers();
app.Run();

public partial class Program { }

```

## Repository source: src/SmartFarm.Web/Views/Shared/_Layout.cshtml at 0418bddc09fceb435b0f77a0114f96c72f8804ff

```
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>@ViewData["Title"] - AniGo</title>
    <link rel="stylesheet" href="~/css/site.css" />
    <script src="~/js/site.js" defer></script>
</head>
<body>
    <a class="skip-link" href="#main-content">Skip to content</a>
    <header class="site-header">
        <a class="brand" href="@Url.Action("Index", "Home")">AniGo</a>
        <nav aria-label="Main navigation">
            @if (User.Identity?.IsAuthenticated == true)
            {
                <a href="@Url.Action("Index", "Farms")">My farms</a>
                <a href="@Url.Action("Index", "Notifications")">Notifications</a>
                <a href="@Url.Action("Profile", "Account")">My account</a>
                <form method="post" asp-controller="Account" asp-action="Logout" class="inline">
                    <button type="submit" class="link-button">Log out</button>
                </form>
            }
            else
            {
                <a href="@Url.Action("Login", "Account")">Log in</a>
                <a href="@Url.Action("Register", "Account")">Create account</a>
            }
        </nav>
    </header>
    <main class="page" id="main-content" tabindex="-1">
        <partial name="_ValidationSummary" />
        @RenderBody()
    </main>
</body>
</html>



```

## Repository source: mobile/SmartFarm.Android/app/src/main/res/values/strings.xml at 0418bddc09fceb435b0f77a0114f96c72f8804ff

```
<resources>
    <string name="app_name">AniGo</string>
    <string name="welcome">Welcome to AniGo</string>
    <string name="welcome_description">Your farming companion.</string>
</resources>

```

## Repository source: README.md at 0418bddc09fceb435b0f77a0114f96c72f8804ff

```
# SmartFarm

SmartFarm is an **in-development farm-management platform** planned for a web application and an Android app. The goal is to help farm owners and field teams manage farms, plots, crop cycles, daily work, supply inventory, harvest records, and weather-related reminders. Android will support selected offline operations with synchronization when connectivity returns.

> **Project status:** Identity, farm/plot management, crop planning, activity/observation monitoring, inventory, and partial harvest recording/voiding now have ASP.NET Core API, MVC, and online-only Android implementations. Harvest totals and audit history use the existing SQL-first Harvest table; crop completion remains a separate operation. Tasks, production-ready media storage, automation, offline synchronization and CI/CD are still future work.

## Start the minimal web and Android preview

Follow the [local smoke test guide](docs/local-smoke-test.md) to launch the
web server, verify `/api/status`, run the Android emulator, and optionally
connect to the existing SQL Server database. See the [reference alignment
review](docs/reference-alignment.md) for verified architectural similarities
and important differences.

## Planned technology stack

| Layer | Technology | Purpose |
| --- | --- | --- |
| Backend / Web | **C# / .NET 10, ASP.NET Core MVC + Web API** | Web interface and API for the Android client |
| Domain / Persistence | **Entity Framework Core 10** | Domain models, database access, eventual migration management |
| Database | **Microsoft SQL Server 2022** | Central application database |
| Authentication | **ASP.NET Core Identity** | User accounts and authorization |
| Background work | **Hangfire** with SQL Server storage | Scheduled tasks and notifications |
| Android | **Kotlin + Jetpack Compose** | Native mobile UI |
| Android local data | **Room** | Offline storage |
| API communication | **Retrofit / OkHttp** | Android-to-API calls |
| Background synchronization | **WorkManager** | Retry and sync queued offline changes |
| External weather | **Weather API integration (provider to be finalized)** | Forecasts and farm advisories |
| Development | **Docker Desktop on macOS; local SQL Server on Windows** | Independent local database environments |
| Collaboration | **Git, GitHub, pull requests, GitHub Actions (planned)** | Source control, review, eventual CI |

## Planned features

- **Accounts and access:** Owner, Manager, and Field Worker roles; users may access multiple farms.
- **Farms and plots:** Register farms, manage farm membership and plots.
- **Crop cycles:** Track planting, active crops, progress, and completion history. At most one active crop cycle per plot.
- **Activities and observations:** Record watering, fertilization, inspections, health observations, and photos.
- **Supply inventory:** Track supplies and stock movements with a transaction history.
- **Harvests:** Record multiple partial harvests; completing a crop cycle remains a separate action.
- **Tasks and alerts:** Planned tasks, scheduled reminders, and weather advisories.
- **Android offline support:** Queue supported field operations locally and sync them when online, including conflict and duplicate-operation handling.

These are design goals; their implementation and end-to-end behavior still require development and testing.

## Repository layout

```text
SmartFarm/
├── src/
│   ├── SmartFarm.Core/           # Domain entities, contracts, interfaces
│   ├── SmartFarm.Infrastructure/ # EF Core, persistence, external integrations
│   ├── SmartFarm.Web/            # MVC web app + API endpoints
│   └── SmartFarm.Worker/         # Hangfire background worker
├── mobile/
│   └── SmartFarm.Android/        # Kotlin / Compose / Room application
├── tests/
│   ├── SmartFarm.SchemaVerification/ # Offline EF model and baseline regression tests
│   ├── SmartFarm.Weather.Tests/ # Provider-independent HTTP client tests
│   ├── SmartFarm.Worker.Tests/  # Background processing configuration tests
│   ├── SmartFarm.Core.Tests/
│   └── SmartFarm.IntegrationTests/
├── database/
│   ├── compose.yaml
│   ├── .env.example
│   ├── 01_CreateDatabase.sql
│   ├── 02_CreateSchema.sql
│   ├── 03_VerifySchema.sql
│   ├── README.md
│   ├── README_macOS.md
│   └── README_Windows.md
├── docs/
└── .github/workflows/
```

Feature folders still contain `.gitkeep` placeholders. The .NET 10 solution includes four backend projects and foundation tests; Android includes its own Gradle project.

## Shared private photos (separate Web and API hosts)

In Development, MVC and API continue using the same `src/SmartFarm.Web/App_Data/observation-photos` folder. For separate deployed instances, mount **one durable private directory** accessible to both processes and set `ObservationPhotos__StoragePath` to that **same absolute mounted path** on **both** hosts (for example, `/mnt/anigo-photos`). Production startup rejects a missing, relative, or public `wwwroot` storage path.

Both hosts need read/write access; keep the mount outside any public web directory. Move existing photos into the shared directory before switching paths, since the SQL database stores photo keys rather than image bytes. Do not move or delete files from the old location until both hosts can open existing photos. A matching path string on two machines is **not** enough unless it refers to the same shared volume.

## Database setup

Developers run their **own** SQL Server instances and use the same version-controlled SQL scripts:

- [macOS: Docker Desktop + DBeaver](database/README_macOS.md)
- [Windows: SQL Server + SSMS/DBeaver, no Docker required](database/README_Windows.md)
- [Database setup overview](database/README.md)

Run the database creation, schema creation, and verification scripts in order as described in those guides. The initial schema defines **16 SmartFarm business tables and 7 ASP.NET Core Identity tables**.

**Important:** The schema was created manually; `__EFMigrationsHistory` is not initialized by these SQL scripts. Before using EF Core Code First migrations, create a carefully verified baseline matching the actual schema. Never run a normal generated `InitialCreate` migration over tables that already exist.

Keep `.env`, database passwords, actual database files, and backups out of Git.

For Phase 6 instructions, see [harvest management](docs/harvest-management.md). For Phase 5 inventory instructions, see [inventory management](docs/inventory-management.md). For Phase 3, see [crop management](docs/crop-management.md). For Phase 2 development, see [farm and plot management](docs/farm-plot-management.md). For Web connection strings and the development connectivity check, see [Backend database connection](docs/backend-database-connection.md). For mapping verification and the approval required before adopting migration history, see [EF Core schema alignment](docs/efcore-schema-alignment.md).

## Git and pull request workflow

Our **two-person team** uses three branch types:

```text
feature/*  ──pull request──>  dev  ──pull request──>  main
individual work              integration          stable
```

- **`main`** is the stable branch; changes arrive from reviewed PRs, normally after integration testing on `dev`.
- **`dev`** is the shared integration branch; create feature branches from its latest commit.
- **`feature/<short-description>`** is a temporary branch for one logical task. Example: `feature/backend-initialization`, `feature/android-initialization`, `docs/project-readme`.
- Do not work directly on `main` or `dev`. Do not force-push or delete protected branches.
- The repository has branch protection for `main` and `dev`. Follow GitHub's displayed approval requirements when opening and merging a PR; another team member should review.
- Prefer **Squash and merge** if linear history is required. Delete a merged feature branch after merging, not `dev` or `main`.

### Terminal workflow (Git CLI)

**First-time clone:**

```bash
git clone https://github.com/Stephware/SmartFarm.git
cd SmartFarm
git fetch origin
git switch dev
git pull --ff-only origin dev
```

**Start a task:** Create a branch from the updated `dev`:

```bash
git switch dev
git pull --ff-only origin dev
git switch -c docs/project-readme
```

Use a task-specific branch name; do not reuse `docs/project-readme` for unrelated changes.

**Review and push your changes:**

```bash
git status
git diff
git add README.md
git commit -m "docs: describe SmartFarm and development workflow"
git push -u origin docs/project-readme
```

For other work, replace the filename, message, and branch name accordingly. Check `git status` before committing; avoid accidentally staging `.env` or generated files.

**Open a pull request:** In GitHub, select **base: `dev`** and **compare: your feature branch**. Request a teammate review. After approval and required checks, use **Squash and merge**.

**After the PR merges:**

```bash
git switch dev
git pull --ff-only origin dev
git branch -d docs/project-readme
```

Optionally delete the merged **remote feature branch** using GitHub's PR page. If the local delete fails, check whether the squash merge was completed before forcing any deletion; do not delete unmerged work.

**Promote tested work:** Open a separate pull request from **`dev` into `main`**, review the integration changes, and merge using an allowed strategy. Sync local branches afterward.

### GUI workflow (GitHub website + GitHub Desktop)

**Option A — GitHub website (documentation or small edits):**

1. Open [SmartFarm](https://github.com/Stephware/SmartFarm), select `dev` in the branch dropdown.
2. Create a new feature branch from `dev` using the branch selector.
3. Switch to the feature branch, open a file, and use **Edit this file** (pencil icon), or **Add file** to create a new one.
4. Commit changes **to the feature branch**, not `main` or `dev`.
5. Open **Pull requests → New pull request**, choose **base: dev** and **compare: your feature branch**.
6. Describe the change, request your teammate's review, resolve feedback, and merge when allowed.

**Option B — GitHub Desktop (normal development):**

1. Install [GitHub Desktop](https://desktop.github.com/). Sign in and choose **File → Clone repository** to clone `Stephware/SmartFarm`.
2. From **Current Branch**, select `dev`. Click **Fetch origin**, then **Pull origin**.
3. Open **Current Branch → New Branch**, enter a descriptive feature name, and make sure it branches from `dev`.
4. Edit files in VS Code or your preferred editor. Review changes in GitHub Desktop's **Changes** tab.
5. Enter a commit summary, click **Commit to <feature-branch>**, then **Publish branch** or **Push origin**.
6. Select **Create Pull Request**; on GitHub verify **base: dev**, then request review.
7. When merged, return to GitHub Desktop, select `dev`, and pull the latest changes before starting another feature.

**Promotion to `main` (GUI):** In GitHub, open **Pull requests → New pull request** with **base: main** and **compare: dev**, then review and merge after testing.

### How to test branch protection safely

This is a **documentation-only** test; it requires no running backend or database:

1. Open a small documentation PR from a feature branch to `dev`.
2. Confirm the PR displays the expected approval/review requirements.
3. Have your teammate review and approve it using **Files changed → Review changes → Approve**.
4. Confirm GitHub permits merging only when the configured requirements are satisfied; use an allowed merge strategy.
5. Verify the file appears on `dev` after merging, and `main` remains unchanged until a separate `dev` → `main` PR is merged.

If merging is blocked, read GitHub's displayed requirement instead of bypassing protection.

## Build, run, and testing status

The backend contains a **.NET 10 solution** with four application projects and foundation test projects. Install the .NET 10 SDK and run from the repository root:

```bash
dotnet --version
dotnet sln SmartFarm.slnx list
dotnet tool restore
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx
dotnet test SmartFarm.slnx
dotnet run --project src/SmartFarm.Web/SmartFarm.Web.csproj
```

Open `http://localhost:5080` to try account registration, login and logout using your configured development database. See the [authentication guide](docs/authentication.md) for API, Android and permission testing. Stop with **Ctrl+C**.

To start the background host independently, run:

```bash
dotnet run --project src/SmartFarm.Worker/SmartFarm.Worker.csproj
```

The Worker starts idle by default. Enable processing only after approved jobs database and Hangfire schema provisioning. Stop with **Ctrl+C**. See the [worker setup](docs/background-worker.md) and [weather configuration](docs/weather-integration.md) guides.

For Android build, emulator tests, and local setup, see [Android initialization](docs/android-initialization.md).

**Not yet available:** business features and their tests, a confirmed weather provider adapter, farm background jobs, and CI.

**Checks that work today:**

- Follow the [database guides](database/README.md) to start or connect to SQL Server, then execute `03_VerifySchema.sql` in DBeaver/SSMS to check the database schema.
- Validate Docker Compose configuration on the macOS setup with `cd database && docker compose config --quiet` after creating your local `.env`.
- Test the GitHub branch/PR workflow using the documentation-only steps above.
- Build and test the Android foundation using the [Android guide](docs/android-initialization.md).

**Planned after project initialization:** business features and tests; GitHub Actions CI for pull requests targeting `dev` and `main`. CI should become a required branch check **only after workflows exist and pass**.

## Collaboration rules

1. Keep changes focused; one feature or setup concern per branch/PR.
2. Never commit production credentials, personal data, database volumes, `.env`, or build outputs.
3. Review your own diff and test what is available before requesting review.
4. Coordinate changes to database schemas, API contracts, and offline sync behavior before merging.
5. Report unfinished functionality honestly in PRs and documentation.

## Next milestones

1. Finish and verify the repository's protected-branch and PR workflow.
2. ~~Initialize the **.NET 10** solution (Core, Infrastructure, Web, Worker).~~ Completed; offline schema tests are available and CI remains planned.
3. ~~Connect the .NET backend to SQL Server and reconcile EF Core with the existing SQL schema.~~ Implemented and verified; baseline history adoption requires approval.
4. ~~Initialize the **Kotlin / Compose** Android project.~~ Foundation build and emulator checks are available.
5. Verify Phase 6 harvest management locally. Task automation and Android offline sync are deferred; CI/CD remains deferred.




```
