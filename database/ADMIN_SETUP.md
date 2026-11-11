# Super Admin bootstrap (operator only)

Phase 1 adds a separate `SuperAdmin` Identity role. It is **not** a farm membership or a public registration option.

1. Back up `SmartFarmDB` and install `database/07_AdminFoundation.sql` (or apply the matching EF migration with verified migration history). Run `database/03_VerifySchema.sql` against the correct database.
2. Create a normal user account through the existing registration flow; verify the person's identity and control of their email out of band.
3. On the trusted **server terminal only**, set `SMARTFARM_ADMIN_BOOTSTRAP=I_UNDERSTAND_ADMIN_ACCESS` for the one process executing:
   `dotnet run --project src/SmartFarm.Web -- --provision-super-admin admin@example.com`
   Replace the email with the approved existing account. Do not set this environment variable permanently. Never embed it in deployment configuration, scripts or source control.
4. Sign in as that account; visit `/admin/security/setup`. Enter the provided authenticator key manually in an authenticator app, confirm the existing password and current TOTP code, and save the one-time recovery codes.
5. Sign in again with password plus authenticator to access `/admin`. The protected portal cannot be accessed with the role alone.

MFA recovery uses one-time codes. There is intentionally no public endpoint to create, revoke, or reset Super Admin access. Future account-management operations must prevent disabling/removing the last active Super Admin, enforce reauthentication, and record an audit event. This phase does not enable system-wide account administration.

For existing SQL-first deployments, install the schema upgrade before launching the Web host. A test/production database must be backed up before any schema change. Do not assume a schema migration was applied by merging code.
