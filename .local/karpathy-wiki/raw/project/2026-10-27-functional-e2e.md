# SmartFarm real workflow and role verification evidence

> Source: Disposable SQL, live MVC/API hosts, Android MainActivity instrumentation, and local repository at 53d4b389b1c14a4d09f1a38c502c54a8b83e89f7
> Collected: 2026-10-27
> Published: Unknown

## Scope and independent SQL verification

Task: registration -> farm -> plot -> crop -> monitoring -> inventory -> harvest -> reports; Owner, Manager, and Field Worker on Web and Android. Only the uniquely task-created SmartFarmEnrollmentTest_E2E_20261027_6a418e database was initialized and written. Web/API test hosts used ports5180/5181 and task-private media/key paths. All temporary test schema substitutions were outside repository source. Existing application database/schema/source were not changed.

Observed direct SQL results: Web valid harvest14.625, valid rows2, voided rows2. Android valid harvest5.000 with3 rows; stock23.000; active crop1. Both farms had one active Manager membership Role1 and one Field Worker membership Role2. SQL assertions confirmed no forbidden Android plot/item/catalog creation and no unauthorized crop completion.

Observed screenshot role-1-inventory.png AFTER uploadPending and empty-queue assertion shows24.000 kg; independent API/SQL result is23.000 kg (stockKg23). This is a stale open-screen display, not lost stock accounting. InventoryViewModel.movement queues and changes notice without reloading item; sync completion has no ViewModel refresh or read-cache invalidation. Online destination re-entry should load fresh data according to source, but was not runtime-asserted.

Browser result: CUA getApp(com.google.Chrome) returned Computer Use permissions are not granted. Real browser click-through/JavaScript/visual verification is blocked; no alternative browser driver or unrelated tabs were used. Web verification is real HTTP forms/rendered HTML/services/SQL, not a browser UI pass. Android Reports/Dashboard screen is absent and unsupported. Existing Web UI unit script tests ran:2 tests passed,0 failed.


## Observed artifact: backend-summary.json

{
  "projects": [
    {
      "report": "e2e_net10.0_20261027231357.trx",
      "passed": 71,
      "failed": 0
    },
    {
      "report": "e2e_net10.0_20261027231401.trx",
      "passed": 138,
      "failed": 0
    },
    {
      "report": "e2e_net10.0_20261027231402.trx",
      "passed": 14,
      "failed": 0
    },
    {
      "report": "e2e_net10.0_20261027231405.trx",
      "passed": 37,
      "failed": 0
    },
    {
      "report": "e2e_net10.0_20261027231406.trx",
      "passed": 47,
      "failed": 0
    }
  ],
  "sqlTests": [
    {
      "name": "SmartFarm.Api.Tests.FarmEnrollmentSqlTests.FarmCreationRetriesARandomJoinCodeCollision_WhenDisposableDatabaseConfigured",
      "outcome": "Passed",
      "duration": "00:00:00.2087714"
    },
    {
      "name": "SmartFarm.Api.Tests.FarmEnrollmentSqlTests.JoiningRegistrationAndConcurrentOwnerApprovalUseRealSql_WhenDisposableDatabaseConfigured",
      "outcome": "Passed",
      "duration": "00:00:00.8178504"
    },
    {
      "name": "SmartFarm.Api.Tests.FarmEnrollmentSqlTests.EnrollmentPersistenceFailureRollsBackTheRealIdentityInsert_WhenDisposableDatabaseConfigured",
      "outcome": "Passed",
      "duration": "00:00:00.1240707"
    },
    {
      "name": "SmartFarm.Api.Tests.BusinessWorkflowSqlTests.OwnerCanManageFarmToHarvestWorkflow_WhenTestDatabaseIsConfigured",
      "outcome": "Passed",
      "duration": "00:00:02.3226405"
    },
    {
      "name": "SmartFarm.Api.Tests.FarmEnrollmentSqlTests.ActiveOwnerAndApplicantReadsCannotDeadlockAcrossFarms_WhenDisposableDatabaseConfigured",
      "outcome": "Passed",
      "duration": "00:00:01.0278303"
    }
  ],
  "totalPassed": 307,
  "failed": 0
}


## Observed artifact: web-report.md

# SmartFarm Web functional/permission verification — 2026-10-27

Result: **77 assertions passed** against disposable Web `http://127.0.0.1:5180` and API `http://127.0.0.1:5181`. No application defect observed within these checks. No production/tracked files changed.

| Area | Actual evidence | Result |
|---|---|---|
| Registration/login/enrollment | Four synthetic accounts registered and logged in through MVC; pending accounts404; Owner MVC approves and promotes Manager | PASS |
| Owner full workflow | MVC farm → plot → shared crop type → planting plan → Active → activity/observation → inventory opening/receipt/consumption → partial harvests/void → reports/CSV → explicit completion | PASS |
| Manager business management | MVC farm edit, plot create, crop plan/activate, activity/observation, receipt/consumption/adjustment/reversal, harvest/void | PASS |
| Owner-only access | Manager crafted membership role/remove and enrollment decision404; Worker promotion/decision404; Manager/Worker join-code API404 and owner panel absent | PASS |
| Field Worker writes | MVC activity, observation, linked consumption and harvest persist | PASS |
| Field Worker forbidden writes | Fully formed anti-forgery-valid MVC requests for farm edit, plot create, inventory create, receipt/adjustment/reversal, crop plan/type/transition, harvest void and self-promotion404 with unchanged persisted state | PASS |
| Nonmember/privacy | Farm/crops/monitoring/inventory/item/harvest/dashboard/report/CSV404; crafted activity and plot writes404 with unchanged state | PASS |
| Input/concurrency/lifecycle | Missing CSRF400; missing consumption activity, insufficient stock and stale inventory version reject without mutation; completed crop blocks activity/harvest; terminal crop cannot reopen | PASS |
| Reports/CSV | Owner/Manager/Worker pages and exports;2 valid,2 voided, net14.625kg; CSV includes4 audit entries; crop/plot/date filtering and reversed-date400 | PASS |
| Real Chrome browser smoke | Native CUA getApp returned `Computer Use permissions are not granted`; no browser tab modified | BLOCKED |

## Evidence method
Python standard-library urllib uses independent cookie jars for each role, GETs the actual MVC form, parses and submits its anti-forgery token, and asserts raw HTTP status without automatically following redirects. All mutations—including registration, enrollment approval and Manager promotion—use MVC. API login plus reads provide an independent persisted-state cross-check using the same test identities; **no API business-state setup writes** were used. Denial checks compare full resource/transaction/member snapshots before/after. These checks exercise rendered Razor pages and real services/SQL, but do not execute browser JavaScript or establish visual/mobile behavior.

Exact successful commands (loopback requires sandbox escalation):

```sh
python3 /private/tmp/smartfarm-e2e-20261027/web/web_e2e.py
python3 /private/tmp/smartfarm-e2e-20261027/web/extra_checks.py
python3 /private/tmp/smartfarm-e2e-20261027/web/role_followup.py
```

The core run contributes54 assertions, inventory/role follow-up11, requested Manager/Worker follow-up12. The final clean run and follow-ups share the identifiers below. Two earlier disposable runs exist in the same isolated database; they need no separate cleanup when that database is removed. Earlier harness-only mismatches were corrected: completed-crop monitoring renders a200 validation view; owner enrollment overview contains pending requests only. `corrected-harness-expectation.txt` preserves the earlier expectation failure, not an application defect. `final_checks.py` was an intermediate recovery script; the3 commands above are the clean reproduction.

## Synthetic identifiers

- **run**: `web-e2e-231753-dc70`
- **farmId**: `da0612c2-50f0-42af-81b0-6a6e001f9797`
- **plotId**: `537260f2-b3bd-4c00-8609-839b9dfa8829`
- **cropTypeId**: `edffa994-028f-4c4c-9086-52204b65d0f5`
- **cycleId**: `89db6e7b-c3f4-44c6-8e6f-faf9087d0fcd`
- **managerCycleId**: `47141894-9c55-46dd-aecb-8158289e6233`
- **itemId**: `e010161d-0f1f-4d4f-ba66-b7289245a694`
- **managerMemberId**: `fe1757fb-9d9f-48a5-9d9d-b579c4f4d8e9`
- **workerMemberId**: `0ba4290f-ba4d-44c2-abf3-ecca3281b7b6`

Accounts: `web-e2e-231753-dc70-owner@example.test`, `web-e2e-231753-dc70-manager@example.test`, `web-e2e-231753-dc70-worker@example.test`, `web-e2e-231753-dc70-outsider@example.test`

Final inventory24kg; report14.625kg,2 valid and2 voided harvest entries. HTTP evidence: 308 requests; statuses {'200': 224, '302': 51, '400': 2, '404': 31}.

Artifacts: `checks.json` (77 assertions), `http-log.json` (sanitized requests/statuses), `results.json` (identifiers and API results, no passwords), `*-report-final.html`, `*-harvests-final.csv`, role farm HTML. `state.json` is private mode0600 and contains only generated test credentials; do not publish it. Folder contains synthetic data only and is mode0700.

Not checked: browser JS/visuals, photos, tasks/weather, Android (owned by separate verification), production deployment or load. Risk: HTTP form checks cannot substitute for real browser interaction; Chrome smoke remains blocked by OS permissions.


## Observed artifact: web-checks.json

[
  {
    "check": "owner MVC registration/login + API identity",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "MVC create rejects missing anti-forgery token",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Owner MVC creates farm persisted in API",
    "result": "PASS",
    "evidence": "da0612c2-50f0-42af-81b0-6a6e001f9797"
  },
  {
    "check": "Owner MVC creates plot persisted in API",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Owner MVC planting plan persisted as Planned",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Owner MVC activates crop",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "owner MVC records monitoring activity",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Owner MVC records observation",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Owner MVC creates inventory opening stock",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Owner MVC receives stock",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Owner MVC consumption links activity and subtracts positive form quantity",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Owner MVC repeated partial harvests keep crop Active",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Owner MVC void preserves audit and excludes quantity",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "manager MVC registration/login + API identity",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "worker MVC registration/login + API identity",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "outsider MVC registration/login + API identity",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "manager pending membership grants no farm access",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "worker pending membership grants no farm access",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Owner MVC approves manager enrollment",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Owner MVC approves worker enrollment",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Owner MVC promotes Manager",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Manager MVC creates plot",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Manager MVC edits farm",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Manager MVC receives stock",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Manager crafted MVC role change blocked with no mutation",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Manager crafted MVC remove member blocked with no mutation",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker farm UI hides administrative forms",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "worker MVC records monitoring activity",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker MVC records observation",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker MVC consumes linked inventory",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker MVC harvest",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker crafted MVC plot creation blocked",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker crafted MVC farm edit blocked",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker crafted MVC inventory item creation blocked",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker crafted MVC receipt blocked",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker crafted MVC adjustment blocked",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker crafted MVC harvest void blocked",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker crafted MVC crop transition blocked",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker crafted MVC self-promotion blocked",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "owner report renders accurate total",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "owner CSV includes void audit and correct valid total",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "manager report renders accurate total",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "manager CSV includes void audit and correct valid total",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "worker report renders accurate total",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "worker CSV includes void audit and correct valid total",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "API report cross-check groups and totals",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "MVC report filter keeps matching harvest totals",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "MVC report rejects reversed date range",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Nonmember cannot read farm/crops/monitoring/inventory/harvest/dashboard/report/CSV",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Nonmember crafted monitoring write blocked without mutation",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Nonmember crafted plot creation blocked without mutation",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Owner MVC explicitly completes crop",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Completed crop blocks monitoring write and retains history",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Completed crop blocks harvest write and retains totals",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Manager MVC plans and activates crop",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker cannot reopen completed crop",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker crafted MVC stock reversal blocked",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "MVC consumption requires active crop activity",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "MVC adjustment cannot make stock negative",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Manager MVC adjusts stock",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "MVC rejects stale inventory row version without mutation",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Manager MVC reverses stock with audit trail",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "manager crafted MVC enrollment decision blocked",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "worker crafted MVC enrollment decision blocked",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Terminal crop cannot be reopened by Owner",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Manager MVC records monitoring activity",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Manager MVC records monitoring observation",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Manager MVC consumes stock linked to activity",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Manager MVC records partial harvest",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Manager MVC voids harvest with preserved audit",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker crafted MVC crop plan blocked",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "Field Worker crafted MVC shared crop-type creation blocked",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "manager cannot read owner join-code endpoint or rendered panel",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "worker cannot read owner join-code endpoint or rendered panel",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "owner final report/CSV preserves net total after Manager void",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "manager final report/CSV preserves net total after Manager void",
    "result": "PASS",
    "evidence": null
  },
  {
    "check": "worker final report/CSV preserves net total after Manager void",
    "result": "PASS",
    "evidence": null
  }
]

## Observed artifact: web-results.json

{
  "run": "web-e2e-231753-dc70",
  "accounts": {
    "owner": "web-e2e-231753-dc70-owner@example.test",
    "manager": "web-e2e-231753-dc70-manager@example.test",
    "worker": "web-e2e-231753-dc70-worker@example.test",
    "outsider": "web-e2e-231753-dc70-outsider@example.test"
  },
  "farmId": "da0612c2-50f0-42af-81b0-6a6e001f9797",
  "plotId": "537260f2-b3bd-4c00-8609-839b9dfa8829",
  "cropTypeId": "edffa994-028f-4c4c-9086-52204b65d0f5",
  "cycleId": "89db6e7b-c3f4-44c6-8e6f-faf9087d0fcd",
  "itemId": "e010161d-0f1f-4d4f-ba66-b7289245a694",
  "managerMemberId": "fe1757fb-9d9f-48a5-9d9d-b579c4f4d8e9",
  "workerMemberId": "0ba4290f-ba4d-44c2-abf3-ecca3281b7b6",
  "finalReport": {
    "farmId": "da0612c2-50f0-42af-81b0-6a6e001f9797",
    "farmName": "web-e2e-231753-dc70-farm",
    "timeZoneId": "Asia/Manila",
    "fromDate": null,
    "toDate": null,
    "plotId": null,
    "cropTypeId": null,
    "validHarvestCount": 2,
    "voidedHarvestCount": 2,
    "unitTotals": [
      {
        "unit": "kg",
        "quantity": 14.625,
        "harvestCount": 2
      }
    ],
    "cropPlotTotals": [
      {
        "cropTypeId": "edffa994-028f-4c4c-9086-52204b65d0f5",
        "cropTypeName": "web-e2e-231753-dc70-tomato",
        "plotId": "537260f2-b3bd-4c00-8609-839b9dfa8829",
        "plotName": "web-e2e-231753-dc70-plot",
        "unit": "kg",
        "quantity": 14.625,
        "harvestCount": 2
      }
    ],
    "monthlyTotals": [
      {
        "month": "2026-10",
        "unit": "kg",
        "quantity": 14.625,
        "harvestCount": 2
      }
    ],
    "entries": [
      {
        "id": "92381ca5-0686-4d79-9a6d-849a6dc38e62",
        "cycleId": "47141894-9c55-46dd-aecb-8158289e6233",
        "plotId": "086b8f44-c457-4183-85ab-12a1a1a170f8",
        "plotName": "web-e2e-231753-dc70-manager-plot",
        "cropTypeId": "edffa994-028f-4c4c-9086-52204b65d0f5",
        "cropTypeName": "web-e2e-231753-dc70-tomato",
        "quantity": 1.25,
        "unit": "kg",
        "harvestedAtUtc": "2026-10-27T15:18:39.364",
        "farmDate": "2026-10-27",
        "isVoided": true,
        "notes": "web-e2e-231753-dc70 manager harvest"
      },
      {
        "id": "5e56b08e-366d-4ba2-bcd5-e2c27d123594",
        "cycleId": "89db6e7b-c3f4-44c6-8e6f-faf9087d0fcd",
        "plotId": "537260f2-b3bd-4c00-8609-839b9dfa8829",
        "plotName": "web-e2e-231753-dc70-plot",
        "cropTypeId": "edffa994-028f-4c4c-9086-52204b65d0f5",
        "cropTypeName": "web-e2e-231753-dc70-tomato",
        "quantity": 4.125,
        "unit": "kg",
        "harvestedAtUtc": "2026-10-27T15:17:54.923",
        "farmDate": "2026-10-27",
        "isVoided": false,
        "notes": "web-e2e-231753-dc70 worker harvest"
      },
      {
        "id": "5b5c823d-17b1-4555-8087-c9614b742f3e",
        "cycleId": "89db6e7b-c3f4-44c6-8e6f-faf9087d0fcd",
        "plotId": "537260f2-b3bd-4c00-8609-839b9dfa8829",
        "plotName": "web-e2e-231753-dc70-plot",
        "cropTypeId": "edffa994-028f-4c4c-9086-52204b65d0f5",
        "cropTypeName": "web-e2e-231753-dc70-tomato",
        "quantity": 2.25,
        "unit": "kg",
        "harvestedAtUtc": "2026-10-27T15:17:54.221",
        "farmDate": "2026-10-27",
        "isVoided": true,
        "notes": "web-e2e-231753-dc70 owner void harvest"
      },
      {
        "id": "42cfae3f-e339-45db-a8cb-0599321c0416",
        "cycleId": "89db6e7b-c3f4-44c6-8e6f-faf9087d0fcd",
        "plotId": "537260f2-b3bd-4c00-8609-839b9dfa8829",
        "plotName": "web-e2e-231753-dc70-plot",
        "cropTypeId": "edffa994-028f-4c4c-9086-52204b65d0f5",
        "cropTypeName": "web-e2e-231753-dc70-tomato",
        "quantity": 10.5,
        "unit": "kg",
        "harvestedAtUtc": "2026-10-27T15:17:54.196",
        "farmDate": "2026-10-27",
        "isVoided": false,
        "notes": "web-e2e-231753-dc70 owner harvest"
      }
    ]
  },
  "checksPassed": 77,
  "managerCycleId": "47141894-9c55-46dd-aecb-8158289e6233",
  "finalInventory": {
    "id": "e010161d-0f1f-4d4f-ba66-b7289245a694",
    "farmId": "da0612c2-50f0-42af-81b0-6a6e001f9797",
    "name": "web-e2e-231753-dc70-fertilizer",
    "category": 2,
    "unit": "kg",
    "description": null,
    "quantityOnHand": 24.0,
    "reorderLevel": 5.0,
    "isLowStock": false,
    "archivedAtUtc": null,
    "rowVersion": "AAAAAAAACDg="
  }
}

## Observed artifact: android-report.md

# SmartFarm actual Android end-to-end verification

Environment: Pixel_9 Android emulator (API 37 / Android 17 beta system image), application compiled with target SDK 36, debug API http://10.0.2.2:5181/. Real isolated SmartFarm API and disposable SQL Server database provided by root agent. No mock HTTP responses. Source code unchanged; external Kotlin instrumentation source and Gradle init script are in this directory.

Method: createAndroidComposeRule<MainActivity>, normal Compose performClick touch injection and text input/replacement, native DatePicker confirmation. Real repositories, Room, WorkManager and Retrofit are used. Roles are checked through actual navigation. Assertions verify returned server data, not merely toast text. All final UI clicks use performClick; no semantics OnClick bypass remains. Explicit uploadPending drives queue completion and server/Room assertions; automatic background scheduling reliability is not established by this test.

Owner UI flow: register standalone account, login, create farm, create plot, create crop type, plan planting, mark active, create activity and observation while Android connectivity is disabled, restore connectivity, upload and confirm exactly one server row per queued operation plus empty local pending queue. Then create inventory item with opening20kg, queue5kg receipt and confirm25kg balance, record3kg harvest and confirm server total.

Fixture setup via real API (not UI): register Manager/Field Worker applicants using owner farm code, owner approves requests, owner promotes one applicant to Manager. These actions only touch this run's uniquely prefixed disposable accounts/farm.

Manager and Field Worker UI flows: login, open actual farm, inspect plots/crops/inventory controls, queue activity, queue1kg partial harvest, select own related activity and queue1kg inventory consumption. Server state and local pending queue are checked. Manager management controls are visible; Field Worker controls to edit farm, add plot, plan/complete cycle, create/edit inventory, receive/adjust stock, and void harvest are absent. Manager's canVoid is true; Field Worker's is false. API denial probes independently verify permission boundaries using valid logged-in role credentials.

Exact server permission rejections expected from source: HTTP404 for non-owner enrollment/role changes; HTTP404 for Field Worker plot creation, crop type creation, stock item creation, receipt, harvest void and crop completion. FarmApiControllerBase intentionally maps FarmError.NotFound to404; services hide inaccessible resources using Missing/NotFound.

Final required business totals: stock23kg (20+5-1-1); harvest5kg (3+1+1), 3 valid harvest records; active crop.

Android Reports/Dashboard UI is unsupported: SmartFarmNavHost contains no report/dashboard destination, and Android Retrofit API interfaces contain no ReportsApi. Do not report Android Reports UI as passed.

Earlier runs corrected external harness issues (AGP9 Kotlin source directory, date-picker lookup, crop-name input vs selection matcher ambiguity, dialog buttons with no scroll parent, lazy-card scrolling, FieldWorker enum display string). Those are not product failures. Gradle connected runs uninstall the app on completion and removed private screenshot artifacts; direct final instrumentation preserves them.

Limits: one emulator/device configuration; selected permission matrix, not every possible endpoint; no Android report screen exists; no photo-picking/camera UI, app-restart authentication, long-lived background scheduling, or actual hardware test in this live workflow. Production code and tracked tests were not edited.

## Displayed balance limitation found during evidence review

The successful test's `role-1-inventory.png` was captured after the explicit `uploadPending()` call and empty pending-queue assertion (test source lines179–181). It still displays24kg and the queued notice, while the final authenticated API read and root's independent SQL check confirm23kg. The test verified server persistence and queue completion; it did **not** verify that the open inventory screen updated to the final balance.

This matches current source behavior: `InventoryViewModel.movement()` only queues the operation and updates receipt/notice/busy fields; it never replaces `state.item` or reloads transactions. `SyncRepository.uploadPending()` submits through SyncApi and removes the Room operation, without notifying InventoryViewModel or invalidating OfflineReadCache. SyncStatusViewModel observes queue changes; navigation passes that pending list to InventoryItemScreen, but quantity still comes from the last-loaded `state.item`. Item loading occurs when entering/re-entering its navigation destination or invoking the error retry handler. `InventoryRepository.itemSnapshot()` is API-first when called, so an online re-entry should retrieve current values by source inspection; this post-sync re-entry/display behavior was not runtime-asserted in this run.

Document the result as: "Inventory movements persisted correctly (final SQL23kg), but the open Android item screen remained at its last-loaded24kg after synchronization. Immediate on-screen balance/history refresh was not verified and is not wired to sync completion in current code." This is an observed stale-display limitation, not evidence of wrong stock accounting or failed upload. It must not be described as verified final on-screen23kg. A source-derived additional implication is that successful sync bypasses the direct InventoryRepository.post() cache invalidation path; offline post-sync cache freshness was not tested.


## Observed artifact: android-results.json

{"status":"passed","tests":1,"failures":0,"seconds":42.872,"farmId":"82a09c93-0552-431c-a11a-e74282e4e55e","cycleId":"77e208eb-c645-4d86-8f61-9b0b2c2f8684","itemId":"9ecf6479-5e30-4650-bbec-7e60f3ceb265","stockKg":23,"harvestKg":5,"validHarvestCount":3,"denialAssertions":10,"androidReportsUi":"unsupported","defaultDebugBuildRestored":true,"taskEmulatorStopped":true}


## Observed artifact: android-instrumentation.log


com.smartfarm.mobile.LiveAndroidE2ETest:.

Time: 42.872

OK (1 test)




## Observed artifact: android-steps.txt

Real MainActivity UI, API=http://10.0.2.2:5181/, run=1793114523516; no mock server
UI action: Create account
UI action: Create/manage my own farm
UI action: Register
UI owner registration succeeded
UI action: Log in
UI action: My farms
UI action: Add a farm
UI action: Create
UI farm created farmId=82a09c93-0552-431c-a11a-e74282e4e55e, owner=android-e2e-owner-1793114523516@example.test
UI action: Plots
UI action: Add plot
UI plot created
UI action: Overview
UI action: Crop management
UI action: Add crop type
UI action: android-e2e-plot-1793114523516
UI action: android-e2e-crop-1793114523516
UI action: Create planting plan
UI action: Mark as planted
UI action: Confirm
UI crop type, plan, active transition cycleId=77e208eb-c645-4d86-8f61-9b0b2c2f8684
UI action: Activities, inspections & photos
DEVICE network=false
UI action: Save activity
UI action: Healthy
UI action: Save observation
UI offline activity+observation saved; Room pending count=2
DEVICE network=true
LIVE uploadPending confirmed activity+observation server rows exactly once and Room queue deletion
UI action: Back to crops
UI action: Back to farm
UI action: Inventory
UI action: Add stock item
UI action: Add inventory item
UI action: android-e2e-stock-1793114523516
UI action: Record stock movement
UI inventory item/opening=20 and queued receipt=5; server balance=25
UI action: Inventory list
UI action: Back to farm
UI action: Crop management
UI action: Harvest records
UI action: Record harvest
UI harvest queued and server total=3 kg; Android Reports UNSUPPORTED (navigation has no report/dashboard destination)
UI action: Back to crop cycles
UI action: Back to farm
API setup only: registered two join applicants; Owner approved both, promoted one Manager
UI action: All farms
UI action: My account
UI action: Log out
UI action: Log in
UI action: Log in
UI action: My farms
UI action: android-e2e-farm-1793114523516
UI action: Plots
UI action: Overview
UI action: Crop management
UI action: Activities, inspections & photos
UI action: Save activity
UI action: Back to crops
UI action: Harvest records
UI action: Record harvest
UI action: Back to crop cycles
UI action: Back to farm
UI action: Inventory
UI action: android-e2e-stock-1793114523516
UI action: Consumption
UI action: android-e2e-plot-1793114523516 · Activity 1 · 2026-10-27 15:22 UTC
UI action: Record stock movement
UI Manager: farm/plot/crop/inventory gating asserted; activity, partial harvest, linked consumption saved to server; queue empty
SERVER DENY Manager owner-only enrollment HTTP 404
SERVER DENY Manager owner-only role change HTTP 404
UI action: Inventory list
UI action: Back to farm
UI action: All farms
UI action: My account
UI action: Log out
UI action: Log in
UI action: Log in
UI action: My farms
UI action: android-e2e-farm-1793114523516
UI action: Plots
UI action: Overview
UI action: Crop management
UI action: Activities, inspections & photos
UI action: Save activity
UI action: Back to crops
UI action: Harvest records
UI action: Record harvest
UI action: Back to crop cycles
UI action: Back to farm
UI action: Inventory
UI action: android-e2e-stock-1793114523516
UI action: Consumption
UI action: android-e2e-plot-1793114523516 · Activity 1 · 2026-10-27 15:22 UTC
UI action: Record stock movement
UI Field Worker: farm/plot/crop/inventory gating asserted; activity, partial harvest, linked consumption saved to server; queue empty
SERVER DENY Field Worker owner-only enrollment HTTP 404
SERVER DENY Field Worker owner-only role change HTTP 404
SERVER DENY Field Worker create plot HTTP 404
SERVER DENY Field Worker crop type HTTP 404
SERVER DENY Field Worker inventory create HTTP 404
SERVER DENY Field Worker receipt HTTP 404
SERVER DENY Field Worker void harvest HTTP 404
SERVER DENY Field Worker complete cycle HTTP 404
UI action: Inventory list
UI action: Back to farm
UI action: All farms
UI action: My account
UI action: Log out
FINAL SERVER farmId=82a09c93-0552-431c-a11a-e74282e4e55e cycleId=77e208eb-c645-4d86-8f61-9b0b2c2f8684 itemId=9ecf6479-5e30-4650-bbec-7e60f3ceb265 stock=23 kg; harvest=5 kg/3 valid rows; active crop; owner+manager+field worker writes verified
DEVICE network=true


## Observed artifact: web-ui-tests.log

TAP version 13
# Subtest: consequential forms submit only after explicit confirmation
ok 1 - consequential forms submit only after explicit confirmation
  ---
  duration_ms: 1.039125
  type: 'test'
  ...
# Subtest: light and dark semantic colors preserve readable text and control boundaries
ok 2 - light and dark semantic colors preserve readable text and control boundaries
  ---
  duration_ms: 1.293375
  type: 'test'
  ...
1..2
# tests 2
# suites 0
# pass 2
# fail 0
# cancelled 0
# skipped 0
# todo 0
# duration_ms 64.421584


## Observed artifact: cleanup-verification.txt

DisposableDatabaseRemoved ExistingSmartFarmDatabasePresent
------------------------- --------------------------------
                        1                                1

(1 rows affected)


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/inventory/InventoryViewModel.kt

package com.smartfarm.mobile.features.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.data.repository.InventoryRepository
import com.smartfarm.mobile.data.sync.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

internal fun inventoryMovementQuantity(type: Int, text: String): BigDecimal {
    val value = text.toBigDecimalOrNull()
    require(type in 2..4 && value != null && value.signum() != 0 &&
        value.stripTrailingZeros().scale() <= 3 && value.abs() <= BigDecimal("999999999999999.999") &&
        (type == 4 || value.signum() > 0)) { "Enter a positive quantity with up to 3 decimals; adjustments may be negative." }
    return if (type == 3) value.negate() else value
}

data class InventoryUiState(
    val farmId: String = "",
    val itemId: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val saveReceipts: Map<String, String> = emptyMap(),
    val workspace: InventoryWorkspaceDto = InventoryWorkspaceDto(emptyList(), 0),
    val item: InventoryItemDto? = null,
    val transactions: List<InventoryTransactionDto> = emptyList(),
    val activities: List<InventoryActivityOptionDto> = emptyList(),
    val offline: Boolean = false,
    val cachedAtUtcMillis: Long? = null
)

class InventoryViewModel(private val repo: InventoryRepository,
    private val sync: SyncRepository) : ViewModel() {
    private val mutable = MutableStateFlow(InventoryUiState())
    val state = mutable.asStateFlow()

    fun clear() { mutable.value = InventoryUiState() }

    fun load(farmId: String) {
        mutable.value = InventoryUiState(farmId = farmId, busy = true, saveReceipts = mutable.value.saveReceipts)
        viewModelScope.launch {
            try {
                val snapshot = repo.listSnapshot(farmId)
                mutable.value = mutable.value.copy(workspace = snapshot.value,
                    offline = snapshot.offline,
                    cachedAtUtcMillis = snapshot.savedAtUtcMillis.takeIf { snapshot.offline })
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not load inventory.")
            } finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    fun loadItem(farmId: String, itemId: String) {
        mutable.value = if (mutable.value.farmId == farmId && mutable.value.itemId == itemId)
            mutable.value.copy(busy = true, error = null) else
            InventoryUiState(farmId = farmId, itemId = itemId, busy = true, saveReceipts = mutable.value.saveReceipts)
        viewModelScope.launch {
            try {
                val item = repo.itemSnapshot(farmId, itemId)
                val history = repo.historySnapshot(farmId, itemId)
                val activityResult = if (item.value.archivedAtUtc == null)
                    repo.activitiesSnapshot(farmId) else null
                val snapshots = listOfNotNull(item, history, activityResult)
                val cached = snapshots.any { it.offline }
                mutable.value = mutable.value.copy(
                    item = item.value, transactions = history.value,
                    activities = activityResult?.value ?: emptyList(),
                    offline = cached,
                    cachedAtUtcMillis = snapshots.filter { it.offline }
                        .minOfOrNull { it.savedAtUtcMillis })
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not load inventory item.")
            } finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    private fun work(after: (() -> Unit)? = null, block: suspend (InventoryUiState) -> Unit) {
        val snapshot = mutable.value
        if (snapshot.busy || snapshot.farmId.isBlank()) return
        mutable.value = snapshot.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                block(snapshot)
                mutable.value = mutable.value.copy(workspace = repo.list(snapshot.farmId))
                if (snapshot.itemId.isNotBlank()) {
                    mutable.value = mutable.value.copy(
                        item = repo.item(snapshot.farmId, snapshot.itemId),
                        transactions = repo.history(snapshot.farmId, snapshot.itemId))
                }
                after?.invoke()
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(
                    error = "Stock change failed. Check values and permissions; reload if inventory has changed.")
            } finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    fun create(name: String, category: Int, unit: String, starting: String, reorder: String) = work {
        repo.create(it.farmId, InventoryItemInputDto(name.trim(), category, unit.trim(),
            reorderLevel = reorder.takeIf { v -> v.isNotBlank() }?.let(::BigDecimal),
            initialQuantity = BigDecimal(starting)))
    }

    fun update(name: String, category: Int, unit: String, description: String, reorder: String) = work {
        val current = requireNotNull(it.item)
        repo.update(it.farmId, it.itemId, InventoryItemInputDto(name.trim(), category, unit.trim(),
            description.trim().ifBlank { null },
            reorder.takeIf { v -> v.isNotBlank() }?.let(::BigDecimal),
            rowVersion = current.rowVersion))
    }

    fun archive(onSuccess: () -> Unit) = work(onSuccess) {
        val current = requireNotNull(it.item)
        repo.archive(it.farmId, it.itemId, current.rowVersion)
    }

    fun movement(type: Int, quantity: String, activity: String?, reason: String) {
        val snapshot = mutable.value
        val current = snapshot.item ?: return
        if (snapshot.busy || snapshot.farmId.isBlank()) return
        mutable.value = snapshot.copy(busy = true, error = null, notice = null)
        viewModelScope.launch {
            try {
                val id = sync.queueInventory(snapshot.farmId, snapshot.itemId, InventoryMovementDto(
                    type, inventoryMovementQuantity(type, quantity), activity, reason.trim().ifBlank { null },
                    current.rowVersion))
                mutable.value = mutable.value.copy(saveReceipts = mutable.value.saveReceipts +
                    ("${snapshot.farmId}/${snapshot.itemId}" to id))
                mutable.value = mutable.value.copy(notice =
                    "Stock change queued. Do not record it again; check offline sync status.")
            } catch (invalid: IllegalArgumentException) {
                mutable.value = mutable.value.copy(error = invalid.message)
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not save the change on this device.")
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    fun reverse(transactionId: String, reason: String) = work {
        val current = requireNotNull(it.item)
        repo.reverse(it.farmId, it.itemId, transactionId,
            InventoryReversalDto(current.rowVersion, reason.trim()))
    }

    class Factory(private val repo: InventoryRepository,
        private val sync: SyncRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InventoryViewModel(repo, sync) as T
    }
}



## Cleanup

DisposableDatabaseRemoved1; ExistingSmartFarmDatabasePresent1. Task host ports5180 and5181 closed. Task emulator stopped, test runner removed, normal debug API URL restored to http://10.0.2.2:5081/ without launching it. Generated Room4.json preserved in temporary artifacts and absent from Git. Git status clean and HEAD unchanged. No application fixes were made by this verification task.
