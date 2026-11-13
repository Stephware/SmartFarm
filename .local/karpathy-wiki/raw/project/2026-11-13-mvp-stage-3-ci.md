# MVP Stage 3 CI test isolation

> Source: PR #126 commit 2ebfa957ecc512a23ce5b042f6b9470e025281ac CI and local follow-up working tree
> Collected: 2026-11-13
> Published: 2026-11-13

The first CI .NET run failed three existing FormRecoveryTests because their synthetic factory did not stub the newly used inventory list dependency. With no database configured, GET Monitoring returned 500. Reproduced locally with ConnectionStrings__SmartFarm empty: three failures, eleven passes. Added an inventory-list stub and explicitly empty database configuration to that factory; all 14 recovery tests then passed. No application implementation changed.

Follow-up full .NET verification: 578 passed, zero failures/skips with database configuration empty; 578 passed, zero failures/skips with both explicitly guarded disposable SQL connections and required-SQL flag. The no-database run does not claim SQL execution; the separate strict SQL run supplies that evidence. Actual native dark inspection/harvest captures were also reviewed; app appearance, density, font scale and night mode restored, and the disposable account signed out. Remote checks remain the merge gate.
