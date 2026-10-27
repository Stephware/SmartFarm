/* Read-only SmartFarmDB verification. Optional EF baseline history is ignored. */
SET NOCOUNT ON;

IF DB_NAME() <> N'SmartFarmDB'
    THROW 50010, 'Select SmartFarmDB before checking its schema.', 1;

-- Check all SmartFarm and Identity objects without editing existing data.
IF EXISTS (
    SELECT 1 FROM sys.tables
    WHERE is_ms_shipped = 0 AND schema_id <> SCHEMA_ID(N'dbo')
)
    THROW 50011, 'Unexpected user tables outside dbo.', 1;

IF (SELECT COUNT(*) FROM sys.tables
    WHERE schema_id = SCHEMA_ID(N'dbo') AND name <> N'__EFMigrationsHistory') <> 24
    THROW 50012, 'Expected 24 SmartFarm and Identity tables.', 1;

IF (SELECT COUNT(*) FROM sys.indexes AS i
    JOIN sys.tables AS t ON t.object_id = i.object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory'
        AND i.is_primary_key = 0 AND i.name IS NOT NULL) <> 48
    THROW 50013, 'Expected 48 application indexes.', 1;

IF (SELECT COUNT(*) FROM sys.foreign_keys AS fk
    JOIN sys.tables AS t ON t.object_id = fk.parent_object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory') <> 42
    THROW 50014, 'Expected 42 foreign keys.', 1;

IF (SELECT COUNT(*) FROM sys.check_constraints AS ck
    JOIN sys.tables AS t ON t.object_id = ck.parent_object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory') <> 38
    THROW 50015, 'Expected 38 check constraints.', 1;

IF (SELECT COUNT(*) FROM sys.columns AS c
    JOIN sys.tables AS t ON t.object_id = c.object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory'
        AND c.system_type_id = TYPE_ID(N'timestamp')) <> 5
    THROW 50016, 'Expected five SQL rowversion columns.', 1;

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
       24 AS AppAndIdentityTables, 48 AS AppIndexes,
       42 AS ForeignKeys, 38 AS CheckConstraints, 5 AS RowVersionColumns;

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
