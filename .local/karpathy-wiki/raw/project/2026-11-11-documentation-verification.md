# Documentation synchronization verification

> Source: local verification commands at unchanged application HEAD ac624be
> Collected: 2026-11-11
> Published: Unknown

No application code, test code, API/schema or business logic was changed. Source snapshot hashes matched 590 non-document tracked files and 13 original raw files. No staged changes. Current branch dev; no commit, push, merge or PR creation.

Documentation check: 41 maintained Markdown files, 370 relative file links, zero missing file targets; all 14 wiki article index dates/coverage consistent. External URL availability and heading anchors are outside this file-target check.

Python 3.10 was used for evidence and Python tests. Schema verifier: 15 passed. Backup/photo checker: 7 passed. Node: 5 tests, 3 pass, 2 fail. Bash syntax and tracked git diff whitespace checks passed. No full .NET build/backend suite, live SQL, emulator, browser interaction, Worker/provider, restore, release or deployment checks performed.

## Existing Node failures

The first failure is document.getElementById is not a function: the confirmation test VM provides querySelector/querySelectorAll only, while current site.js initializes navigation with getElementById. The contrast test treats every :root block as a complete palette; later overrides inherit missing tokens, so a lookup is undefined and hex.slice fails. These source/test files are unchanged; no rendered contrast or confirmation behavior verdict follows. The failures are a current verification limit, left unfixed under documentation-only scope.

## Evidence checker

```text
# Evidence check

## Fidelity suspects

(none)

## Evidence errors
(none)

## Unreferenced raw files
(none)

## Summary
0 fidelity suspect(s), 0 evidence error(s), 0 unreferenced raw file(s)
```

## Schema-verifier tests

```text
test_between_does_not_swallow_boolean_and (test_verify_schema.PredicateTests) ... ok
test_distinct_grouping_remains_distinct (test_verify_schema.PredicateTests) ... ok
test_enrollment_string_states_preserve_literals (test_verify_schema.PredicateTests) ... ok
test_sql_server_expansion_matches_without_losing_grouping (test_verify_schema.PredicateTests) ... ok
test_unsupported_predicate_rejected (test_verify_schema.PredicateTests) ... ok
test_cli_exit_codes_for_match_drift_and_unsupported (test_verify_schema.SchemaTests) ... ok
test_column_collation_drift_rejected (test_verify_schema.SchemaTests) ... ok
test_column_drift_detected (test_verify_schema.SchemaTests) ... ok
test_index_filter_grouping_drift_detected (test_verify_schema.SchemaTests) ... ok
test_literal_comma_and_comment_markers_preserved (test_verify_schema.SchemaTests) ... ok
test_named_defaults_and_identity_seed_are_significant (test_verify_schema.SchemaTests) ... ok
test_only_safe_default_cast_equivalence (test_verify_schema.SchemaTests) ... ok
test_optional_ef_history_is_ignored_but_other_tables_are_not (test_verify_schema.SchemaTests) ... ok
test_original_and_bracketed_sql_match (test_verify_schema.SchemaTests) ... ok
test_unsupported_schema_syntax_fails_closed (test_verify_schema.SchemaTests) ... ok

----------------------------------------------------------------------
Ran 15 tests in 0.081s

OK
```

## Backup-verifier tests

```text
test_corrupt_backup_is_rejected (test_verify_photo_backup.PhotoBackupVerificationTests) ... ok
test_empty_backups_cannot_be_claimed_as_verified (test_verify_photo_backup.PhotoBackupVerificationTests) ... ok
test_matching_binary_photos_pass (test_verify_photo_backup.PhotoBackupVerificationTests) ... ok
test_missing_backup_file_is_rejected (test_verify_photo_backup.PhotoBackupVerificationTests) ... ok
test_same_or_nested_directory_is_rejected (test_verify_photo_backup.PhotoBackupVerificationTests) ... ok
test_symlinks_are_not_trusted_as_backup_files (test_verify_photo_backup.PhotoBackupVerificationTests) ... ok
test_unexpected_backup_file_is_rejected (test_verify_photo_backup.PhotoBackupVerificationTests) ... ok

----------------------------------------------------------------------
Ran 7 tests in 0.125s

OK
```

## Node checks

```text
TAP version 13
# Subtest: location search is only called by a deliberate user action
ok 1 - location search is only called by a deliberate user action
  ---
  duration_ms: 1.050833
  type: 'test'
  ...
# Subtest: choosing a search result fills location and six-decimal coordinates
ok 2 - choosing a search result fills location and six-decimal coordinates
  ---
  duration_ms: 0.291625
  type: 'test'
  ...
# Subtest: consequential forms submit only after explicit confirmation
not ok 3 - consequential forms submit only after explicit confirmation
  ---
  duration_ms: 1.376416
  type: 'test'
  location: '/Users/stephen/Documents/SmartFarm/tests/web-ui/site.test.cjs:7:1'
  failureType: 'testCodeFailure'
  error: 'document.getElementById is not a function'
  code: 'ERR_TEST_FAILURE'
  name: 'TypeError'
  stack: |-
    evalmachine.<anonymous>:84:30
    evalmachine.<anonymous>:120:3
    Script.runInContext (node:vm:149:12)
    Script.runInNewContext (node:vm:154:17)
    Object.runInNewContext (node:vm:310:38)
    TestContext.<anonymous> (/Users/stephen/Documents/SmartFarm/tests/web-ui/site.test.cjs:15:12)
    Test.runInAsyncScope (node:async_hooks:214:14)
    Test.run (node:internal/test_runner/test:1047:25)
    Test.start (node:internal/test_runner/test:944:17)
    startSubtestAfterBootstrap (node:internal/test_runner/harness:296:17)
  ...
# Subtest: light and dark semantic colors preserve readable text and control boundaries
not ok 4 - light and dark semantic colors preserve readable text and control boundaries
  ---
  duration_ms: 1.108375
  type: 'test'
  location: '/Users/stephen/Documents/SmartFarm/tests/web-ui/site.test.cjs:25:1'
  failureType: 'testCodeFailure'
  error: "Cannot read properties of undefined (reading 'slice')"
  code: 'ERR_TEST_FAILURE'
  name: 'TypeError'
  stack: |-
    luminance (/Users/stephen/Documents/SmartFarm/tests/web-ui/site.test.cjs:28:19)
    contrast (/Users/stephen/Documents/SmartFarm/tests/web-ui/site.test.cjs:38:46)
    TestContext.<anonymous> (/Users/stephen/Documents/SmartFarm/tests/web-ui/site.test.cjs:41:23)
    Test.runInAsyncScope (node:async_hooks:214:14)
    Test.run (node:internal/test_runner/test:1047:25)
    Test.processPendingSubtests (node:internal/test_runner/test:744:18)
    Test.postRun (node:internal/test_runner/test:1173:19)
    Test.run (node:internal/test_runner/test:1101:12)
    async startSubtestAfterBootstrap (node:internal/test_runner/harness:296:3)
  ...
# Subtest: copy code reports success and selects the code when clipboard access fails
ok 5 - copy code reports success and selects the code when clipboard access fails
  ---
  duration_ms: 0.635375
  type: 'test'
  ...
1..5
# tests 5
# suites 0
# pass 3
# fail 2
# cancelled 0
# skipped 0
# todo 0
# duration_ms 60.099334
```
