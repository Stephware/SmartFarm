# Farmer registration and farm-owner approval

## Intent

Farmers should register through the registration page and receive access after the farm owner approves their request in the dashboard. The user confirmed that approval belongs to each farm's owner and that farmers identify the farm with a code supplied by that owner.

Today, accounts already use email/password registration and login. Membership is granted through a protected invitation link. Authority is farm-specific: Owner, Manager and Field Worker; there is no global administrator role.

## Approaches

1. **Farm code and approval — selected by the user.** The owner shares a reusable code, the farmer submits it, and the owner approves or rejects the request. There is no public farm directory. Knowing the code never grants access by itself.
2. **Public searchable farm list.** Easier discovery, but exposes farm names and requires a public search surface. The user selected the code approach instead.

Keep the existing ASP.NET Identity, Razor MVC, API, Compose screens, and farm authorization services. No new framework or dependency is needed.

## Registration and farmer experience

- Web and Android registration offer “Join an existing farm” and “Create/manage my own farm.” Joining requires a farm code; owner registration keeps the existing create-farm path. This distinction controls onboarding, not a new global role.
- The farmer supplies name, email, password, password confirmation and code. Normalize code case and separator/outer whitespace consistently. Invalid or archived-farm codes produce a field error without creating a partial account.
- Account creation and initial join-request persistence are one transaction. The response confirms registration and that farm access awaits approval. No membership is created yet.
- Farmers may sign in while pending. “My farms” shows their request's farm name and Pending, Approved or Rejected status. Pending users cannot read farm records through either UI or API.
- Existing accounts can enter a farm code from “My farms” to request membership without registering again. This also supports requesting access to another farm.
- Repeated submissions do not create duplicate pending requests. Rejected users, or users whose membership was removed, may deliberately submit a new request; historic decisions remain recorded.
- Approved farmers become Field Workers. Owners can subsequently promote them with the existing role-management workflow. Existing memberships and ownership remain intact.

## Owner experience

- Each active farm has a code visible to its owner in the dashboard and farm-member management. The code is displayed as selectable text with clear instructions; no invitation URL or token needs to be handled by a farmer.
- The owner dashboard includes pending requests with farmer name, email and submission date, and named Approve/Reject actions. Empty and failure states are explicit.
- Only the farm owner can view applicants or decide requests. Managers and Field Workers cannot approve, obtain the owner-only code view, or enumerate pending applicants.
- Approval creates an active Field Worker membership and records the decision atomically. Repeated or simultaneous decisions cannot create duplicate memberships or overwrite an earlier decision. Existing active membership is handled safely.
- Archived farms and disabled applicants cannot be approved. A request is always bound to its stored farm and applicant; neither can be substituted in a decision request.
- Rejecting a request creates no membership. Both parties can see the result through their normal screens; email delivery and push notifications are outside this change.

## Data and service changes

- Add a stable, randomly generated, unique farm join code to the existing Farm model, with a non-destructive backfill for existing farms. Display grouping is presentation only. Code rotation/expiry is outside this change: a code allows requesting, never joining automatically.
- Add a FarmJoinRequest table with identity, FarmId, UserId, status, requested timestamp, optional reviewer and reviewed timestamp. Enforce one pending request per farm/user and valid decision states. Preserve decision history.
- Reuse the existing repository/unit-of-work and farm access patterns. Implement request and decision logic in shared services used by MVC and API; authorization must be checked at the service boundary.
- Add focused API contracts for authenticated request submission, the caller's request status, owner-only pending requests/code retrieval, and decisions. Owner approval APIs work with the stored request identity and cannot accept a client-selected member role.
- Extend the shared registration service and Web/API/Android registration input contracts with the joining choice/code while keeping owner signup available. Update all callers and test fixtures affected by those contracts.
- Prepare an explicit upgrade script for populated SQL-first databases, fresh-schema changes, EF mappings/model snapshot and verification updates using repository conventions. Do not rerun the destructive schema-creation script or automatically alter the user's populated database.

## Replacing invitation links

Replace the Web and Android invitation creation/acceptance controls with code submission and approval. Retire the old invitation-acceptance route so outstanding links cannot bypass the new approval workflow; return a clear retired-flow response. Preserve existing accounts, farm records, memberships and owner role management. Update invitation-related contracts, tests and documentation accordingly.

This intentionally changes the old onboarding contract. Clients must be updated with the new API and database schema before using code-based requests.

## Validation

- Test registration with a valid code, invalid/archived code, duplicate email and persistence failure; failed registration must not leave a user without its required request.
- Test pending and rejected users cannot access farm data, and only the correct owner can see and decide requests. Reject spoofed farm/user IDs and requested roles.
- Test approve/reject, repeated decisions, concurrent requests/decisions, disabled applicants, archived farms, membership removal followed by reapplication, and preservation of existing memberships.
- Test rendered Web registration, pending status and dashboard actions, including antiforgery and retained non-password inputs on failure.
- Test API/Android request contracts, signup choice/code validation, pending status and owner approval UI. Retain the narrow-screen, keyboard and draft-recovery behavior from the UI work.
- Verify the upgrade against a populated disposable test database where available and run schema checks. Report any unavailable SQL/device checks explicitly.
- Report existing baseline test failures separately; do not treat them as new regressions or repair unrelated behavior under this feature.

## Builder decisions

Exact route names, DTO names, code length/grouping, SQL upgrade file numbering, and small UI placement details follow existing conventions. Use a sufficiently unpredictable code with a unique constraint and collision handling. Preserve drafts on errors, use semantic labels/status announcements and the current shared layouts. No public farm directory, global administrator role, account-wide login approval, new notification transport or automatic database deployment is included.
