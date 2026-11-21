# Weather integration and saved forecasts

Reviewed against `dev` at `ac624be` on 2026-11-11. Implementation status below is source-backed; it is not a fresh runtime acceptance result.

OpenMeteo is implemented and selected by checked-in configuration. The
provider-independent typed service uses `OpenMeteoAdapter` to validate/map external
forecasts. It is a no-key provider in current use; keep any future credentials out
of Git and client applications.

## Configuration and boundaries

`Weather:Provider=OpenMeteo`, `Weather:BaseAddress=https://api.open-meteo.com/`,
`Weather:TimeoutSeconds=15` are current defaults. Timeout accepts 1–120 seconds.
Optional header-name/key settings are a paired capability, not a requirement for
OpenMeteo. Configuration constrains HTTPS/expected origin; requests cannot redirect
or escape origin. HTTP request logging is disabled and provider errors sanitized.
Caller cancellation propagates through send/parse; malformed data fails safely.

Worker fetches for farms with coordinates, stores valid near-term forecasts,
derives/resolves advisories and deduplicated in-app notifications. Adapter requests
four days; Worker stores usable today through today+2. Read service's larger date
window does not mean five days were ingested. Missing wind/probability data must
not falsely clear an existing warning.

Web/API/Android read saved forecasts and advisories; opening weather does not call
OpenMeteo. Forecast-retrieved time and local cache-saved time are different. UI
shows missing coordinates, no snapshot, stale forecast and offline-copy states.
Android marks forecasts stale after 12 hours, malformed timestamps or an implausible
future retrieval time; coordinate changes invalidate weather cache.

## Verification

```sh
dotnet test tests/SmartFarm.Weather.Tests/SmartFarm.Weather.Tests.csproj
```

Fake-handler tests validate parsing/origin/timeouts; they do not prove provider
availability or Worker ingestion. A live check needs configured enabled Worker,
separate jobs database, application database and a coordinate-equipped test farm.
Use [Worker setup](background-worker.md) and [automation](../.local/karpathy-wiki/wiki/project/automation.md).
No live provider request or running-job result was produced by this docs task.
