# Branded transactional email

Use the supplied reference composition with AniGo's existing #134D30 pine/#CBA14E gold mark and warm neutral surfaces. Reuse SMTP, queues, throttles, token issuance and event triggers. Implement only the audited password-reset and password-change events in English/Filipino; other listed workflows have in-app notifications or retired presentation, not email triggers.

1. Add rendering tests: encoding, optional values, safe actions, localization, text alternative and MIME construction.
2. Implement a dependency-free shared table/inline-style renderer and typed security templates. Keep reset tokens out of preview text and details; preserve plain-text reset-link extraction.
3. Integrate at existing message construction and SMTP MIME boundary, with AniGo display name. No schema/provider/configuration changes.
4. Add a Development-only, no-send CLI for fictional previews; rasterize the existing mark for email client compatibility. Inspect desktop/narrow/long/missing renderings. Actual Gmail/Outlook/Apple Mail rendering remains separate from browser checks.
5. Run affected tests/builds and code review; update this branch/PR. Prepare release instructions without restarting production. Real sends require authorized recipient; no live tokens in previews.

## Verified implementation

- Complete: dependency-free shared layout, two supported events in English/Filipino, AniGo MIME sender name, plain/HTML alternatives and PNG brand asset; existing trigger/queue/provider/security behavior retained.
- Complete: Development-only no-send CLI with six fictional HTML/text previews and offline logo/index. Browser checks cover 12 size/content combinations plus dark preference; desktop/320px/dark renders inspected.
- Complete: 693 Release solution tests pass (SQL-dependent cases require separate SQL configuration); seven recovery/backend cases also pass with required SQL enabled on a new disposable local database, removed afterward. Ten rendering/MIME/security cases pass. Independent focused review found no important issue.
- Complete: one fictional pre-release preview accepted by existing Resend SMTP and found in the authorized Gmail inbox. It used an embedded mark because the production hosted PNG has not yet been published. No live reset token, password change, configuration modification, service restart or automatic workflow was involved.
- Release artifacts prepared outside Git. Production template activation remains pending the approved release process; current services remain unchanged. Real Outlook/Apple Mail/Gmail mobile visual checks remain manual. Full latest-head CI is tracked in [email PR #156](https://github.com/Stephware/SmartFarm/pull/156).

The preceding deployment head `4f23b090` finished all seven CI jobs successfully before this email commit. New-head CI must be checked separately; prior green results do not establish the email commit is green.
