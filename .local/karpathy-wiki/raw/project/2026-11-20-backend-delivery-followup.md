# Backend delivery follow-up checks

Source: PR149 backend branch, disposable SQL execution and GitHub Actions run 44567721507
Collected: 2026-11-20
Published: 2026-11-20

A fresh database using the existing SmartFarmEnrollmentTest_ prefix reproduced a new-test fixture guard incompatibility. The fixture now accepts both approved disposable prefixes, SmartFarmEnrollmentTest_ and SmartFarmEnrollmentBackend_, still rejecting populated database names. Full strict disposable-SQL Release suite passed 649 tests after this correction. A later complete Release run including the final demo-code assertion again passed 649 tests, zero failed/skipped.

Final contract review found demo join codes used non-hexadecimal letters despite the existing enrollment service requiring 12 ASCII hexadecimal characters. The integrity test failed for all three demo farms. DemoDataset now derives deterministic hexadecimal codes from its existing fixed IDs. A new disposable database passed complete model/catalog comparison, native checksum backup and VERIFYONLY, then applied 40 synthetic records. Live API login verified owner/manager/worker/applicant farm isolation; submitting the applicant's code reused the already-pending enrollment without extra records. The populated local SmartFarmDB was not touched. Existing seed rows are never overwritten, including earlier disposable test datasets.

GitHub Actions run 44567721507 passed .NET, Android, Tailwind and backup checks. Its reviewed-visual job compared all 30 full-page landing/login/registration light/dark captures at 320/375/768/1024/1440: 26 reported 0.000% pixel difference, four narrow landing captures 0.001%. It failed only the source fingerprint guard because frontend PR148 changed _Layout.cshtml without updating the review manifest. Current source SHA-256 is 65513673ffe659dad9f42e0b42cf1e43a155ee61695d697caad42547eb1e0d47; the stale recorded SHA-256 was 330d377586364b284606a727eedbc795061ed3ef82cb1970d81d4eac7fa4f9ab.

Codex inspected a contact sheet of all 30 actual CI viewport captures. Only tests/browser/baselines/review.json's _Layout fingerprint and explanatory provenance note were refreshed. Golden PNGs, comparison thresholds, frontend pages/navigation/assets and browser assertions were not changed. The Chromium smoke job was still in progress at this observation; final CI must be inspected after the follow-up push, not inferred from the screenshot comparison.

Email/Gemini live verification, populated-local schema alignment/seeding, physical devices, production Worker/deployment and full restore remain outside the verified result. This follow-up adds no dependency, migration or business-data mutation.
