/* Read-only SmartFarmDB verification. Optional EF baseline history is ignored. */
SET NOCOUNT ON;

IF DB_NAME() <> N'SmartFarmDB'
    THROW 50010, 'Select SmartFarmDB before checking its schema.', 1;

-- Check the base schema, required admin audit and optional finance extension without editing data.
DECLARE @financeTable int = CASE WHEN OBJECT_ID(N'dbo.FarmFinanceEntry', N'U') IS NULL THEN 0 ELSE 1 END;
IF OBJECT_ID(N'dbo.AdminAuditLog', N'U') IS NULL
    THROW 50071, 'Missing required AdminAuditLog table. Apply 07_AdminFoundation.sql.', 1;
IF EXISTS (
    SELECT 1 FROM sys.tables
    WHERE is_ms_shipped = 0 AND schema_id <> SCHEMA_ID(N'dbo')
)
    THROW 50011, 'Unexpected user tables outside dbo.', 1;

IF (SELECT COUNT(*) FROM sys.tables
    WHERE schema_id = SCHEMA_ID(N'dbo') AND name <> N'__EFMigrationsHistory') <> 25 + @financeTable
    THROW 50012, 'Unexpected SmartFarm table count.', 1;

IF (SELECT COUNT(*) FROM sys.indexes AS i
    JOIN sys.tables AS t ON t.object_id = i.object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory'
        AND i.is_primary_key = 0 AND i.name IS NOT NULL) <> 51 + (3 * @financeTable)
    THROW 50013, 'Expected 51 application indexes.', 1;

IF (SELECT COUNT(*) FROM sys.foreign_keys AS fk
    JOIN sys.tables AS t ON t.object_id = fk.parent_object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory') <> 44 + (4 * @financeTable)
    THROW 50014, 'Expected 44 foreign keys.', 1;

IF (SELECT COUNT(*) FROM sys.check_constraints AS ck
    JOIN sys.tables AS t ON t.object_id = ck.parent_object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory') <> 39 + (3 * @financeTable)
    THROW 50015, 'Expected 39 check constraints.', 1;

IF (SELECT COUNT(*) FROM sys.columns AS c
    JOIN sys.tables AS t ON t.object_id = c.object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory'
        AND c.system_type_id = TYPE_ID(N'timestamp')) <> 6
    THROW 50016, 'Expected six SQL rowversion columns.', 1;

-- Disabled or untrusted constraints can look present while not protecting data.
IF EXISTS (
    SELECT 1 FROM sys.foreign_keys AS fk
    JOIN sys.tables AS t ON t.object_id = fk.parent_object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo')
        AND (fk.is_disabled = 1 OR fk.is_not_trusted = 1)
)
    THROW 50018, 'Foreign key checks are disabled or untrusted.', 1;

IF EXISTS (
    SELECT 1 FROM sys.check_constraints AS ck
    JOIN sys.tables AS t ON t.object_id = ck.parent_object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo')
        AND (ck.is_disabled = 1 OR ck.is_not_trusted = 1)
)
    THROW 50019, 'Check constraints are disabled or untrusted.', 1;

IF EXISTS (
    SELECT 1 FROM sys.indexes AS i
    JOIN sys.tables AS t ON t.object_id = i.object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo')
        AND i.name IS NOT NULL AND i.is_disabled = 1
)
    THROW 50020, 'An application index is disabled.', 1;

-- Verify security audit indexes and its actor relationship.
IF NOT EXISTS (SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.AdminAuditLog') AND name = N'IX_AdminAuditLog_OccurredAt')
    OR NOT EXISTS (SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.AdminAuditLog') AND name = N'IX_AdminAuditLog_Actor_Date')
    OR NOT EXISTS (SELECT 1 FROM sys.foreign_keys
    WHERE parent_object_id = OBJECT_ID(N'dbo.AdminAuditLog') AND name = N'FK_AdminAuditLog_Actor')
    THROW 50072, 'Missing a required admin audit index or foreign key.', 1;

-- These unique indexes protect important business rules and retry safety.
IF EXISTS (
    SELECT 1
    FROM (VALUES
        (N'Farm', N'UX_Farm_JoinCode', 0),
        (N'FarmJoinRequest', N'UX_FarmJoinRequest_Pending', 1),
        (N'FarmMember', N'UX_FarmMember_Active', 1),
        (N'CropCycle', N'UX_CropCycle_OneActivePerPlot', 1),
        (N'FarmTask', N'UX_FarmTask_Dedupe', 1),
        (N'InventoryTransaction', N'UX_InventoryTransaction_Reverses', 1),
        (N'Notification', N'UX_Notification_User_Dedupe', 0),
        (N'WeatherAdvisory', N'UX_WeatherAdvisory_Daily', 0)
    ) AS expected(TableName, IndexName, MustBeFiltered)
    LEFT JOIN sys.tables AS t ON t.schema_id = SCHEMA_ID(N'dbo')
        AND t.name = expected.TableName
    LEFT JOIN sys.indexes AS i ON i.object_id = t.object_id
        AND i.name = expected.IndexName
    WHERE i.index_id IS NULL OR i.is_unique <> 1
        OR (expected.MustBeFiltered = 1 AND i.has_filter <> 1)
)
    THROW 50017, 'A required unique business index is missing or incorrectly configured.', 1;

SELECT N'PASS' AS SchemaCheck, DB_NAME() AS ConnectedDatabase,
       25 + @financeTable AS AppAndIdentityTables,
       51 + 3 * @financeTable AS AppIndexes,
       44 + 4 * @financeTable AS ForeignKeys,
       39 + 3 * @financeTable AS CheckConstraints,
       6 AS RowVersionColumns;

SELECT name AS TableName
FROM sys.tables
WHERE schema_id = SCHEMA_ID(N'dbo')
ORDER BY name;

SELECT fk.name AS ForeignKeyName,
       OBJECT_NAME(fk.parent_object_id) AS ChildTable,
       OBJECT_NAME(fk.referenced_object_id) AS ParentTable
FROM sys.foreign_keys AS fk
WHERE OBJECT_SCHEMA_NAME(fk.parent_object_id) = N'dbo'
ORDER BY ChildTable, ForeignKeyName;
