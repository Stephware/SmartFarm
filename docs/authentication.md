# Authentication, enrollment and authorization

Reviewed against `dev` at `ac624be` on 2026-11-11. Implementation status below is source-backed; it is not a fresh runtime acceptance result.

## Accounts and hosts

Web uses Identity cookie forms with antiforgery; API uses opaque Identity bearer
tokens, not JWT. Configure SQL per [backend setup](backend-database-connection.md).
Web runs on 5080, API 5081; Android debug emulator targets API at 10.0.2.2:5081.

Current Web/Android signup asks for full name, email and password only; account
creation grants no farm rights. Sign in, then create a farm or submit a code from
My farms. Earlier join-at-signup/confirmation-required instructions are historical.
API accepts optional `confirmPassword` and validates a match only if supplied;
it no longer takes `joinFarm`/`farmCode` for registration.

Passwords require 10 characters, upper/lowercase and a digit (symbol optional).
Unique email is enforced; five failed attempts lock out for ten minutes. Cookie
is HttpOnly/Lax, secure outside Development, with sliding expiration. Disabled
users are rejected. API access lifetime is 30 minutes, refresh seven days; refresh
validates expiry, security stamp and enabled user.

| Endpoint | Contract |
| --- | --- |
| POST `/api/auth/register` | `fullName,email,password`, optional matching `confirmPassword`; 201, separate sign-in |
| POST `/api/auth/login` | `email,password`; tokenType/accessToken/expiresIn/refreshToken |
| POST `/api/auth/refresh` | `refreshToken`; rotated access/refresh credentials |
| GET `/api/auth/me` | Bearer-authenticated current enabled account |
| GET `/api/farms/{farmId}/access` | Live farm-specific permission, or not found |

Both hosts rate-limit registration (5/10 minutes/IP), login (20/minute/IP) and
relevant authenticated enrollment actions (10/minute/user); no waiting queue.
Rejection is 429 with Retry-After when available. Limits are per-process, not a
distributed abuse-control service.

## Farm permissions and enrollment

Owner is `Farm.OwnerUserId`; active membership Role=1 is Manager, Role=2 Field
Worker. These codes differ from internal access-level enum values. Global Identity
roles never grant farm access. Active account/farm/membership is checked live.
Owner/Manager manages business data; Owner alone manages members, codes and decisions
and archives the farm. Workers record permitted field actions and close assigned
tasks. See operation-specific [rules](../.local/karpathy-wiki/wiki/project/features.md).

| Enrollment route | Action |
| --- | --- |
| GET/POST `/api/farm-join-requests` | Own history / submit `{farmCode}` |
| GET `/api/farms/{farmId}/join-requests` | Owner code, pending applicants and history |
| POST request `/{requestId}/decision` | Owner `{approve}` decision |
| POST request `/{requestId}/cancel` | Applicant cancels own Pending request |
| POST farm `/join-code/regenerate` | Owner `{currentCode}` guarded code rotation |

Request routes above nest under farm `/join-requests`. Codes normalize case/hyphens
and authorize requesting only. Pending/rejected/cancelled records grant no access.
Approval creates Field Worker atomically; promotion is separate. Same decisions
can replay; conflicting final decisions cannot overwrite history. Existing pending
requests survive code rotation. Enrollment creates in-app notices; no email/push.
Legacy invitation routes return 410 Gone. All enrollment UI requires live requests;
owner codes/applicants are not shared offline snapshots.

## Deployment and tests

Apply only reviewed necessary [SQL upgrades](../database/README.md); startup never
migrates/adopts history. Web/API deployments require environment-matching protected
Data Protection keys and durable private shared photo storage; use HTTPS.
Android tokens remain memory-only and logout cannot instantly revoke issued bearer
tokens. Refresh/logout races are guarded. Secure persistence, email confirmation,
general password recovery remain future work. Web Super Admin TOTP and one-time
recovery-code login now exist under a separate MFA policy; see
[current source status](../.local/karpathy-wiki/wiki/project/current-source-status.md).

Run relevant Auth/API tests, real disposable-SQL enrollment/concurrency tests and
client role flows. CI alone may omit SQL/device execution. See
[verification commands](../.local/karpathy-wiki/wiki/project/development.md).
Never place sample/live tokens, passwords or private runtime data into shared docs.
