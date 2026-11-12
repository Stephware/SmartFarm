# Documentation publication source check

> Source: SmartFarm dev at 3c5a8b0; inspected repository paths and local Git history
> Collected: 2026-11-12
> Published: 2026-11-12

This publication carries the earlier documentation changes onto current remote dev, retaining historical raw snapshots. It is not a repeated full application/runtime acceptance audit. Latest source, not earlier article counts, is authoritative.

Git history includes Super Admin PR87–92, Android usability PR93–101 and Web UI PR102–112. Their presence is provenance only, not a fresh test or deployment result. No application code/schema change is authorized or included by this documentation publication.

Current Web remains Razor MVC and focused JavaScript, with shared _UiEmptyState/_UiStatusChip partials, farmer-operations styles and farm-navigation.js. Tailwind 4.3.0 uses tw:-prefixed utilities without Preflight. package.json provides npm ci and npm run css:build; committed anigo-tailwind.css is loaded before reference-ui.css in both application/admin layouts. The latter remains the effective theme layer. No PRODUCT.md or DESIGN.md exists at this revision.

Current Android navigation uses Farm, Tasks, Record, Crops and More. Record is a native sheet; More supplies secondary operations. Farm switching has exit confirmation for applicable forms. Compose/StateFlow/Room/WorkManager and memory-only authentication remain. This publication did not rerun Android UI/device acceptance.

The MVC Admin area includes dashboard/users/farms/audit/monitoring/reports/support/announcements. Its SuperAdminMfa policy requires authenticated SuperAdmin role plus amr=mfa; provisioning is an explicitly guarded operator CLI action for an existing enabled account, not public registration. AccountController includes TOTP and one-time recovery-code sign-in. Farm permissions remain separate from global administration. Support is user-scoped on ordinary MVC pages; admin support has internal messages and versioned state/priority updates. Announcements have admin draft/publish/archive and user-facing reads. Setup/live acceptance remains operator work; use database/ADMIN_SETUP.md with current source rather than its historical phase-only omissions.

Current base CREATE parser counts: 25 tables, 214 columns, 25 primary_keys, 44 foreign_keys, 39 checks, 51 indexes. Base now includes AdminAuditLog. Optional finance adds one table; SupportTicket/SupportTicketMessage add two; PlatformAnnouncement adds one. Complete EF source expectations are 29 tables, 256 columns, 53 foreign keys, 60 indexes, 48 checks and eight rowversions, from SchemaModelTests. These are source expectations, not measurements of any live database. Reviewed extensions are 06_FarmFinance.sql, 07_AdminFoundation.sql (already in current base), 08_SupportManagement.sql and 09_PlatformAnnouncements.sql; do not blindly replay all upgrades or adopt migration history.

Important source limitation: 03_VerifySchema.sql currently requires AdminAuditLog and checks exactly 25 + optional finance tables, with corresponding base/finance counts. It does not cover the full support/announcement schema and can reject an otherwise intended extended database. It is not full-model acceptance. Review complete live metadata with current mappings and extension contracts; do not silently alter SQL/verifier code in this docs-only task.

Current Verify SmartFarm CI runs .NET build/tests, Node web tests, local illustration validation, Android build/JVM and device-test compilation, public Chromium responsive/a11y smoke, Tailwind generation consistency, and PR before/after Chromium visual captures. Phase A workflow checks recovery/schema scripts. CI does not provide a live SQL instance, Android emulator, authenticated production/browser fixture, external provider acceptance, backup restore or deployment. Workflow inventory is not a claim that this publication's remote runs have completed.

Privacy review found only example.test synthetic addresses, test credentials also present in historical tracked test sources, source-code parameter names and configuration placeholders among candidate matches. Private verification directory, environment files, runtime logs/cookies/keys/photos, generated files and the unrelated local Gradle daemon configuration are excluded from publication. Personal installation paths identify this machine's existing skill/checker locations, not deployable secrets.

## Inspected source hashes
- `src/SmartFarm.Web/Views/Shared/_Layout.cshtml`: `78e1fe7d4fd95a2d90156d9fb14b28ee2d0fa14b33200a7e9b6dbc6bdddeb508`
- `src/SmartFarm.Web/Authentication/SuperAdminSecurity.cs`: `bc26fc6371ce5743db4335a8244ea0dceedb62e8006ad7991f0f85a27487ae6d`
- `src/SmartFarm.Web/Authentication/SuperAdminProvisioner.cs`: `174a7103dedddb0750bf3f1d416b917c3fec999d4b4f4097af54af48d837edf5`
- `src/SmartFarm.Web/Controllers/AccountController.cs`: `ed25c0c36e66ec130f2e3d365066b727c142fe144f345e9097050f09b6c910a4`
- `src/SmartFarm.Web/Controllers/SupportController.cs`: `b9ab46ac2a090007ae361fb91bbda12a414d366156820be41f60a0348236f555`
- `src/SmartFarm.Web/Areas/Admin/Controllers/SupportController.cs`: `0188e99704704b4ad581d336d878de49477e6db0ca87b5ed20900e5becfa7f15`
- `src/SmartFarm.Web/Areas/Admin/Controllers/AnnouncementsController.cs`: `4aff28ed8545c497bdf7e94742348bb7226095e2609f3798856ce0434320daab`
- `mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/navigation/SmartFarmNavHost.kt`: `e984276d43e709e161a1e2315c758971b2d169a76424d35f470d11dd7c295e9d`
- `mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/navigation/FarmWorkspaceBar.kt`: `e486d21f177afeb92602e4f85acb1293fce9293ab8b462e94f8c0d5d7bd5b985`
- `package.json`: `8e2f00ec3b3af3b744dd0ba01ebf424dc3d5a679813e4e75ffde31e49e552340`
- `src/SmartFarm.Web/Styles/anigo-tailwind.css`: `9e215ae3e088f58fdc3ae651baa09aebe701d5e85be87116739a06ce5ed836bf`
- `.github/workflows/verify-smartfarm.yml`: `df0a5c15251b1d4cc1b1059095211b205f83473a3e298f1d4c1a7b53bce0b9c4`
- `database/02_CreateSchema.sql`: `38cc9c937781e5bb8e07454abb1dbdffc685c853c86f4cefac9551efbc8efd20`
- `database/03_VerifySchema.sql`: `1b8bcb6cd9e1ba0adc7f26ad8a75fcc4c6d9a1b60bf73f189e4acd82d95e8081`
- `database/07_AdminFoundation.sql`: `f2a873cfc327b57ea04ddfcdf0f52463eeed7eec6b52846c0fd8a0c9b7421488`
- `database/08_SupportManagement.sql`: `07e8cd355227aad2a7c9db666f75c4b41c9be197570e96c42f35b6aaf0880a05`
- `database/09_PlatformAnnouncements.sql`: `3d1a53b1adf775c0af021468d97160ec04c808c8eb7e780f2c855c807a890eda`
- `tests/SmartFarm.SchemaVerification/SchemaModelTests.cs`: `92284f3bd70fa12e0aa383509deef12883026becbb2ca2404b4d9ac1b4439b7e`
