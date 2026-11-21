# AniGo transactional email

The shared `AniGoEmailLayout` renders text-only content models into encoded HTML and a plain-text alternative. It supports a heading/preheader, personalized greeting, paragraphs, optional information rows, primary action and secondary note. Tables/inline styles, a 640px maximum width, Outlook conditional wrapper, solid high-contrast button, and AniGo's existing #134D30/#CBA14E mark keep the layout readable without JavaScript or external stylesheets. The PNG mark is a deterministic raster of the existing SVG, not a new logo.

## Audited workflows

Only `password.reset` and `password.changed` currently send email. Both now have English/Filipino templates through the existing queue and SMTP sender, including authorized full name, expiry/security guidance and Philippine timestamp. No registration/verification, welcome, farm invitation, task, weather, harvest, support or admin email triggers were introduced. Existing in-app notifications and retired support presentation stay unchanged.

`SecurityEmailMime` sets the display name AniGo and builds UTF-8 multipart/alternative (plain first, HTML second). The existing `Email:From=noreply@anigo.website`, Resend host/port/credentials, TLS, timeout, failure handling, throttles, token issuance/expiry and queue behavior remain intact. Capture/Disabled modes keep their behavior. Four-argument `SecurityEmail` construction remains compatible.

## Safe previews

From the repository root:

```sh
DOTNET_ENVIRONMENT=Development dotnet run --project tools/SmartFarm.EmailPreview -- /absolute/new/preview-directory
```

Open `index.html`. The output contains six fictional HTML/text examples: reset/change in both languages, long/missing detail content, and a minimal message without optional sections. The local PNG enables offline viewing. The tool refuses non-Development execution or an existing output path, loads no application settings/database, and has no send method. Never expose captured recovery emails or live reset tokens through a preview route.

## Security and verification

All dynamic content is HTML-encoded; no caller-provided HTML fragments are accepted. Actions require absolute HTTPS with no embedded credentials/control characters. Only an explicit Development flag permits loopback HTTP, preserving existing local recovery behavior. Production action URLs still originate from the validated configured recovery origin, not request headers. Tokens appear only where required in the action URL and plain-text counterpart, never in preheaders or information rows. User names are not placed in subjects.

Automated checks cover encoding, invalid URLs, optional fields, localized templates, single reset-link extraction, MIME alternatives and the Development exception. Existing recovery/expiry/failure tests also run against a unique disposable local database with Capture mode; no populated database or production account is used. Browser rendering covers 320/390/1280px, long values, missing fields and dark preference. This is not a claim of testing the real Outlook, Apple Mail or Gmail mobile rendering engines; inspect actual-client output before broad rollout.

## Release

Continue through [email PR #156](https://github.com/Stephware/SmartFarm/pull/156)/current feature branch. Build and review Web/API artifacts including the new public `images/branding/anigo-email-mark.png`; preserve protected runtime configuration. No SQL migration, provider replacement, credential change or Worker update is needed. Both Web and API construct recovery messages, so release them together. Respect the active deployment's no-interruption requirement: prepare a parallel release, verify it, then use a validated graceful Caddy route switch and allow the old in-memory email queues to drain. Do not overwrite running DLLs, restart SQL/Worker, or expose new preview endpoints. Activation must follow the approved release process; committing/testing these templates does not mean production was updated.

A pre-release real preview uses fictional content, no live reset token, and an embedded copy of the same mark while the new hosted PNG awaits release. It does not alter the production templates' absolute HTTPS image URL. Real sends require the verified configuration and an authorized recipient; do not automatically send samples from CI.
