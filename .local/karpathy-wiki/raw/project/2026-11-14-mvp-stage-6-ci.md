# MVP Stage 6 Android CI synchronization follow-up

> Source: PR #129 Actions run 43584712800 and its test/logcat artifact; source and fresh local Android verification
> Collected: 2026-11-14 (Asia/Manila)
> Published: 2026-11-14

The initial Stage 6 head 2c985f9 passed six CI jobs. The manually dispatched Android emulator job completed 118 tests with 116 passing and two failures: FullAppJourneyTest immediately asserted the Record sheet absent after device Back; NavigationInputsTest could not discover the native date-picker positive button. CI logcat records the native Back event timeout and a real picker window added before accessibility discovery, later leaked at test teardown. The unchanged focused classes passed all six tests locally; local Android 17 success does not substitute for CI Android 15 acceptance.

GitHub records PR #129 merged by the Stephware account at 2026-11-14T10:33:01Z, commit 6982780fa6d20e61c48525c97f95b7ae37c7cd4f, while this follow-up was being verified. This executing agent issued no merge action, informed the user and preserved the external work. That merge's source tree is identical to 2c985f9. The follow-up uses a new branch from merged dev. The user's explicit-approval requirement still applies: do not merge its new PR automatically or infer approval from the prior external merge.

Test-only correction: wait for the Record sheet to be displayed and native input idle, send actual device Back, then wait up to 10 seconds for observed dismissal before retaining the absence assertion. After opening the actual framework picker, commit Compose/native window work before the existing UI Automator ID/text discovery and confirmation. ISO-date equality, actual button click, real navigation and sign-out assertions remain. No retry/skip, fake callback, business/API/Room/schema/application change or new dependency.

Fresh local debug build, 64 JVM and all 118 device tests passed with zero failures/skips after this correction. Remote all-seven verification must be checked on the follow-up PR's exact final head. Prior Stage 6 MVC/API/SQL/Worker/browser/native offline evidence remains applicable; the immutable Stage 6 snapshot is unchanged. Provider timeouts, absent farmer sessions and other release limits are unchanged.
