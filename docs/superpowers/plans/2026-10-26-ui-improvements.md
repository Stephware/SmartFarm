# SmartFarm UI improvements implementation plan

> **For agentic workers:** Use superpowers:executing-plans to implement each group. Merge its PR to dev before starting the next group.

**Goal:** Apply the approved Web and Android UI review in five independently reviewable groups.

**Architecture:** Keep Razor MVC and Jetpack Compose, existing business services, permissions, offline queue and database schema. Reuse native controls and installed dependencies.

**Tech Stack:** ASP.NET Core MVC, CSS/JavaScript, Kotlin, Jetpack Compose Material 3, Room/WorkManager.

**Spec:** The eleven findings in the UI UX Pro Max review in this conversation, approved for implementation on October 26, 2026.

## Global constraints

- No database or authorization changes, new UI frameworks, or new dependencies.
- Preserve submitted values on failure; reset drafts only after confirmed local persistence.
- Confirm consequential actions with the affected record's name.
- Keep offline and server-confirmed status distinct.
- One group branch, PR and merge at a time, based on latest dev.
- Known baseline failures: DashboardAndHarvestReportUseTheProtectedApiRoutes; DevelopmentHealthExplainsMissingDatabaseConnection. Report them separately; do not hide them or expand this UI task to repair them.

## Review focus

- Invalid numeric/date values and long notes remain recoverable after failed submission.
- Cancelling a destructive action must not invoke its callback.
- Rotation, keyboard visibility and failed photo persistence must preserve drafts.
- Queued records are visible without pretending the server confirmed them.
- Read-only roles, dark mode, large text and narrow screens retain usable controls.

### Task 1: Web form recovery and accessible labels

**Files:** Web crop/monitoring controllers, view models and views; shared validation UI and script; repeated unlabeled Web fields; Auth.Tests form recovery tests.

**Interfaces:** Keep service contracts; bind each multi-form input with its own prefix. Re-render the same authorized workspace on validation/business errors, redirect only on success.

- [x] Write HTTP tests for failed crop planning and monitoring forms, including attempted values and authorization.
- [x] Run tests (expected: failure due to redirect/lost values).
- [x] Preserve model state and form drafts, add field errors and focused linked summaries, associate repeated labels with unique controls.
- [x] Run .NET tests and diff checks; review, commit, push, create and merge PR.

### Task 2: Safe consequential actions

**Files:** Android farm/inventory screens and reusable confirmation UI; Web crop/stock actions; Compose instrumentation tests.

**Interfaces:** Keep existing callbacks. Confirmation only invokes them after explicit approval; cancellation does nothing.

- [x] Write Compose tests for confirm/cancel behavior and disabled busy state; run RED.
- [x] Add named confirmations, preserve existing harvest/crop dialog patterns, distinguish dangerous actions.
- [x] Build Android, run unit and available device tests; review and merge PR.

### Task 3: Android drafts and offline feedback

**Files:** Android authentication, monitoring, harvest/inventory screens/view models; queue display; Compose tests.

**Interfaces:** Observe local queue through existing SyncRepository. Distinguish saved locally, pending and server-confirmed records; retain drafts until saving succeeds.

- [x] Write failure/success draft tests and queued-record visibility tests; run RED.
- [x] Make auth forms keyboard-safe; preserve photo capture state; reset successful drafts and show pending records with sync access.
- [x] Build, run tests, review and merge PR.

### Task 4: Navigation and practical data entry

**Files:** Farm workspace/navigation, Android native date/time-zone and selection controls, stock movement inputs, retry/empty-state copy.

**Interfaces:** Reuse existing APIs and permissions; add task/notification entry points where supported. Keep signed quantities at the service boundary while making consumption/receipt input positive for users.

- [x] Write behavior tests for date/quantity inputs and navigation; run RED.
- [x] Group farm work separately from settings; use native date pickers, accessible selection and time-zone choices; add context-preserving retry and user-facing copy.
- [x] Build/test both platforms, review and merge PR.

### Task 5: Visual consistency and responsive layouts

**Files:** Web CSS/shared layout, Android theme/shared screen layout and list screens; responsive/semantics tests.

**Interfaces:** Preserve completed workflows; green primary, neutral secondary, error/destructive roles; adaptive gutters and bounded forms; lazy lists with stable record keys.

- [x] Check narrow-screen and large-text behavior using available UI tools/tests.
- [x] Align color roles, action hierarchy, spacing, headings and responsive list/form layouts without new dependencies.
- [x] Run final .NET/Android checks, review and merge PR; report any unavailable visual/device checks.

**Final verification:** Web UI checks pass (2/2). Android unit tests pass (12/12); emulator tests pass 28/29, with only the existing invalid-JPEG photo fixture failure. .NET passes 273/275, with the two baseline failures listed above. New coverage verifies long-list virtualization, immediate access to create forms, 375dp dark registration at 200% font scale, landscape submit access, and semantic palette contrast. Narrow/landscape emulator screenshots were inspected; manual Web browser, TalkBack and physical tablet checks were unavailable.

**Review:** The final review found create/join forms opening after all list records. Two regression tests reproduced this; conditional forms now precede the records. No other critical or important review findings remain. Each group is integrated through its own PR to dev.
