/* Run in DBeaver with SmartFarmDB selected. */
SELECT DB_NAME() AS ConnectedDatabase;
SELECT COUNT(*) AS TotalTables FROM sys.tables WHERE schema_id = SCHEMA_ID(N'dbo');
-- Expected TotalTables = 23
SELECT name AS TableName
FROM sys.tables
WHERE schema_id = SCHEMA_ID(N'dbo')
ORDER BY name;
SELECT fk.name AS ForeignKeyName,
       OBJECT_NAME(fk.parent_object_id) AS ChildTable,
       OBJECT_NAME(fk.referenced_object_id) AS ParentTable
FROM sys.foreign_keys AS fk
WHERE SCHEMA_NAME(OBJECTPROPERTYEX(fk.parent_object_id, 'SchemaId')) = N'dbo'
ORDER BY ChildTable, ForeignKeyName;
