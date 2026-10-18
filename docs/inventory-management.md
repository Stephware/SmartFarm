# Phase 5 — Inventory management

This phase uses the existing `dbo.InventoryItem` and `dbo.InventoryTransaction`
tables, `CropActivity` relationships, Identity and farm-scoped access. It follows
SmartFarm's Core contracts → EF Core service → API/MVC → Android
Retrofit/repository/ViewModel/Compose layering.

No SQL schema changes, migrations, Hangfire jobs or CI/CD pipelines are added.

## Inventory roles

- Owner and Manager: create inventory items, update metadata, record receipts
  and adjustments, reverse transactions, and archive zero-stock items.
- Field Worker: view inventory/history and record **consumption linked to a
  valid active crop activity** in the same farm.
- Nonmember: no access. All endpoints recheck farm permissions on the server.
- Archived items retain read-only history; they cannot accept new transactions.

## Quantity and audit rules

`InventoryItem.QuantityOnHand` is a derived balance updated **atomically** in
one EF Core `SaveChanges` with each append-only `InventoryTransaction`.
Every balance-changing action must create an auditable ledger entry.

| Code | Type | QuantityChange | Conditions |
| --- | --- | --- | --- |
| 1 | Opening | Positive | Added automatically when item created with opening stock |
| 2 | Receipt | Positive | Manager/Owner |
| 3 | Consumption | Negative | Active-crop `CropActivityId` required; workers allowed |
| 4 | Adjustment | Positive or negative, not zero | Manager/Owner and required reason |
| 5 | Reversal | Opposite sign of original | Separate endpoint, owner/manager, reason, references original |

- Stock cannot go below zero. Up to **3 decimal places** and SQL
  `decimal(18,3)` magnitude are enforced.
- Each mutation requires the item's current Base64 SQL `rowversion`. A
  concurrent/stale update yields **409 Conflict**; reload before retrying.
  The SQL check constraint also rejects negative stock.
- Existing unique index `UX_InventoryTransaction_Reverses` allows only **one**
  reversal per original. Reversals cannot themselves be reversed.
- A receipt/adjustment/consumption cannot silently overwrite stock. Inventory
  transactions are never edited or deleted.
- Nonzero inventory must be consumed, adjusted, or otherwise accounted for
  before item or farm archiving. The existing farm archive checks now include
  live inventory stock.
- Low stock is indicated when an active item has a configured reorder level
  and its quantity is at or below the level. No notifications are sent yet.
- `Unit` and category are metadata; converting between different units is
  not included. Avoid changing the unit of an item with an established history
  unless historic quantities retain the same meaning.

## Bearer API

All under `/api/farms/{farmId}/inventory`.

| Method | Route suffix | Action |
| --- | --- | --- |
| GET / POST | `/` | List / create item |
| GET | `/activities` | Recent active-crop activities available for consumption |
| GET / PUT / DELETE | `/{itemId}` | Get, update metadata, archive (version required) |
| GET | `/{itemId}/transactions` | Immutable transaction history |
| POST | `/{itemId}/transactions` | Post receipt, consumption, or adjustment |
| POST | `/{itemId}/transactions/{transactionId}/reverse` | Reverse a transaction once |

Examples: receipt uses
`{"transactionType":2,"quantityChange":5.000,"rowVersion":"..."}`.
Consumption uses
`{"transactionType":3,"quantityChange":-2.000,"cropActivityId":"...","rowVersion":"..."}`.
Adjustments require a `reason`; reversal requires `reason` and
`rowVersion`. A receipt with an activity ID is deliberately rejected.

The web UI is under **My farms → Farm → Inventory**, and Android's farm screen
includes an **Inventory** entry. Android remains online-only; queued offline
inventory changes are deferred until the Room/WorkManager sync phase.

## Test locally

Start the existing SQL Server and configure the Web connection string, then:

```bash
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx
dotnet test SmartFarm.slnx
dotnet run --project src/SmartFarm.Web
```

Android from `mobile/SmartFarm.Android`:

```bash
./gradlew assembleDebug testDebugUnitTest
```

With two accounts, test:

1. Owner creates a fertilizer item with opening stock 10.000.
2. Owner posts a receipt +5; quantity becomes 15.000.
3. Worker records a crop fertilization activity (Phase 4), then consumes
   -2 tied to that activity; quantity becomes 13.000.
4. Consumption without a linked activity, or linked to a different farm, is rejected.
5. A -14 adjustment is rejected for insufficient stock. A permitted -1
   adjustment with a reason records a new ledger entry.
6. Owner reverses one receipt once; repeated reversal is rejected.
7. Two sessions submit stock changes with the same old `rowVersion`:
   one succeeds and the other receives 409.
8. Reorder threshold triggers low-stock status at the configured level.
9. Field Worker cannot edit item details, receive stock, adjust or reverse.
10. Archived items preserve history; archived farms cannot hide positive
    active inventory.

Local SQL Server and Android runtime execution are still required before
merging. Do not run `EnsureCreated`, `EnsureDeleted` or a new migration.
