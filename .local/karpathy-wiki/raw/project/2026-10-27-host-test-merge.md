# Host test fixes merged into dev

> Source: Local Git verification and confirmed GitHub PR response, https://github.com/Stephware/SmartFarm/pull/46
> Collected: 2026-10-27
> Published: Unknown

GitHub confirmed PR 46 is closed and merged into dev.

5d69ea1ed40a782d89dd98ff0db0bea96dc05b5d
Fix report route and database health tests
Keep report query strings in tests and isolate the database health check from local settings.

 tests/SmartFarm.Api.Tests/DatabaseHealthTests.cs       | 13 +++++++------
 tests/SmartFarm.Auth.Tests/BusinessClientRouteTests.cs |  2 +-
 2 files changed, 8 insertions(+), 7 deletions(-)

## Preservation check

Local dev and origin/dev are synchronized. The merged Git tree exactly equals the tested branch tree at 02848b82ac1e4bf21ca366ce961ad62269e49b87. Only the two requested test files differ from PR base d4f7ab245547a06b62d099a82f91016bfc3b4f6f. Working tree is clean. Ignored local documentation copies remain byte-identical to their pre-cleanup source. Wiki and AGENTS.md remain local and untracked.
