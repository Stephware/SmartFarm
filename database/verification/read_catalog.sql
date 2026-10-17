/* Read-only metadata export. Save the single JSON result as a local UTF-8 file.
   Connect to the existing SmartFarmDB; this script makes no database changes. */
SET NOCOUNT ON;

IF DB_NAME() <> N'SmartFarmDB'
    THROW 50003, 'Connect to SmartFarmDB before exporting schema metadata.', 1;

SELECT
    JSON_QUERY(COALESCE((
        SELECT name, collation_name, compatibility_level
        FROM sys.databases WHERE database_id = DB_ID()
        FOR JSON PATH
    ), N'[]')) AS [database],
    JSON_QUERY(COALESCE((
        SELECT s.name AS [schema], t.name AS [table], t.temporal_type, t.is_memory_optimized
        FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id
        WHERE t.is_ms_shipped = 0 ORDER BY s.name, t.name
        FOR JSON PATH
    ), N'[]')) AS tables,
    JSON_QUERY(COALESCE((
        SELECT s.name AS [schema], t.name AS [table], c.column_id, c.name AS [column],
            ty.name AS [type], c.max_length, c.precision, c.scale, c.is_nullable,
            c.is_identity, c.is_computed,
            CONVERT(bigint, ic.seed_value) AS identity_seed,
            CONVERT(bigint, ic.increment_value) AS identity_increment,
            dc.name AS default_name, dc.definition AS default_sql,
            cc.definition AS computed_sql, c.collation_name
        FROM sys.tables t
        JOIN sys.schemas s ON s.schema_id = t.schema_id
        JOIN sys.columns c ON c.object_id = t.object_id
        JOIN sys.types ty ON ty.user_type_id = c.user_type_id
        LEFT JOIN sys.identity_columns ic ON ic.object_id = c.object_id AND ic.column_id = c.column_id
        LEFT JOIN sys.default_constraints dc ON dc.object_id = c.default_object_id
        LEFT JOIN sys.computed_columns cc ON cc.object_id = c.object_id AND cc.column_id = c.column_id
        WHERE t.is_ms_shipped = 0 ORDER BY s.name, t.name, c.column_id
        FOR JSON PATH
    ), N'[]')) AS columns,
    JSON_QUERY(COALESCE((
        SELECT s.name AS [schema], t.name AS [table], i.name, i.type_desc,
            i.is_unique, i.is_primary_key, i.is_unique_constraint, i.filter_definition,
            i.is_disabled, ic.key_ordinal, ic.is_included_column, ic.is_descending_key,
            c.name AS [column]
        FROM sys.tables t
        JOIN sys.schemas s ON s.schema_id = t.schema_id
        JOIN sys.indexes i ON i.object_id = t.object_id
        JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
        JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
        WHERE t.is_ms_shipped = 0 ORDER BY s.name, t.name, i.name, ic.index_column_id
        FOR JSON PATH
    ), N'[]')) AS indexes,
    JSON_QUERY(COALESCE((
        SELECT s.name AS [schema], t.name AS [table], fk.name, c.name AS [column],
            fkc.constraint_column_id, rs.name AS referenced_schema, rt.name AS referenced_table,
            rc.name AS referenced_column, fk.delete_referential_action_desc,
            fk.update_referential_action_desc, fk.is_disabled, fk.is_not_trusted
        FROM sys.foreign_keys fk
        JOIN sys.tables t ON t.object_id = fk.parent_object_id
        JOIN sys.schemas s ON s.schema_id = t.schema_id
        JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
        JOIN sys.columns c ON c.object_id = fkc.parent_object_id AND c.column_id = fkc.parent_column_id
        JOIN sys.tables rt ON rt.object_id = fkc.referenced_object_id
        JOIN sys.schemas rs ON rs.schema_id = rt.schema_id
        JOIN sys.columns rc ON rc.object_id = fkc.referenced_object_id AND rc.column_id = fkc.referenced_column_id
        ORDER BY s.name, t.name, fk.name, fkc.constraint_column_id
        FOR JSON PATH
    ), N'[]')) AS foreignKeys,
    JSON_QUERY(COALESCE((
        SELECT s.name AS [schema], t.name AS [table], cc.name, cc.definition,
            cc.is_disabled, cc.is_not_trusted
        FROM sys.check_constraints cc
        JOIN sys.tables t ON t.object_id = cc.parent_object_id
        JOIN sys.schemas s ON s.schema_id = t.schema_id
        ORDER BY s.name, t.name, cc.name
        FOR JSON PATH
    ), N'[]')) AS checks,
    JSON_QUERY(COALESCE((
        SELECT s.name AS [schema], t.name AS [table], tr.name, tr.is_disabled
        FROM sys.triggers tr
        JOIN sys.tables t ON t.object_id = tr.parent_id
        JOIN sys.schemas s ON s.schema_id = t.schema_id
        ORDER BY s.name, t.name, tr.name
        FOR JSON PATH
    ), N'[]')) AS triggers
FOR JSON PATH, WITHOUT_ARRAY_WRAPPER;
