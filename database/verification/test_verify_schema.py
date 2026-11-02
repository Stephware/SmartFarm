from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

import verify_schema as verifier


SIMPLE = """
CREATE TABLE dbo.Sample (
    Id UNIQUEIDENTIFIER NOT NULL,
    Name NVARCHAR(20) NOT NULL,
    Status TINYINT NOT NULL CONSTRAINT DF_Sample_Status DEFAULT (0),
    CONSTRAINT PK_Sample PRIMARY KEY (Id),
    CONSTRAINT CK_Sample_Status CHECK (Status IN (0,1,2))
);
CREATE UNIQUE INDEX UX_Sample_Name ON dbo.Sample(Name) WHERE Status = 1;
"""


class PredicateTests(unittest.TestCase):
    def test_enrollment_string_states_preserve_literals(self):
        self.assertEqual(verifier.predicate("Status IN (N'Pending', N'Approved', N'Rejected')"),
                         verifier.predicate("([Status]=N'Rejected' OR [Status]=N'Pending' OR [Status]=N'Approved')"))
        self.assertEqual(verifier.predicate("Status = 'Pending'"), verifier.predicate("Status = N'Pending'"))
        self.assertNotEqual(verifier.predicate("Status = N'Pending'"), verifier.predicate("Status = N'Rejected'"))

    def test_sql_server_expansion_matches_without_losing_grouping(self):
        self.assertEqual(verifier.predicate('A IN (1,2) AND B BETWEEN -3 AND 4'),
                         verifier.predicate('([A]=(2) OR [A]=(1)) AND [B]>=(-3) AND [B]<=(4)'))
        self.assertEqual(verifier.predicate('A NOT IN (1,2) OR B IS NOT NULL'),
                         verifier.predicate('NOT ([A]=(2) OR [A]=(1)) OR [B] IS NOT NULL'))

    def test_distinct_grouping_remains_distinct(self):
        self.assertNotEqual(verifier.predicate('(A=1 OR B=2) AND C=3'),
                            verifier.predicate('A=1 OR (B=2 AND C=3)'))

    def test_between_does_not_swallow_boolean_and(self):
        self.assertEqual(verifier.predicate('A IS NULL OR A BETWEEN 0 AND 100'),
                         verifier.predicate('A IS NULL OR (A >= 0 AND A <= 100)'))

    def test_unsupported_predicate_rejected(self):
        for sql in ('A + 1 = 2', "Name LIKE 'x%'", 'A=1; DROP TABLE dbo.Sample'):
            with self.subTest(sql=sql), self.assertRaises(verifier.UnsupportedSql):
                verifier.predicate(sql)


class SchemaTests(unittest.TestCase):
    def test_original_and_bracketed_sql_match(self):
        quoted = SIMPLE.replace('dbo.Sample', '[dbo].[Sample]').replace('NVARCHAR', 'nvarchar')
        self.assertEqual(verifier.parse_schema(SIMPLE), verifier.parse_schema(quoted))

    def test_column_drift_detected(self):
        changed = SIMPLE.replace('NVARCHAR(20)', 'NVARCHAR(40)')
        differences = verifier.differences(verifier.parse_schema(SIMPLE), verifier.parse_schema(changed))
        self.assertEqual(len(differences), 1)
        self.assertEqual(differences[0]['object'], ('Sample', 'Name'))

    def test_index_filter_grouping_drift_detected(self):
        before = SIMPLE.replace('WHERE Status = 1', 'WHERE (Status=1 OR Status=2) AND Id IS NOT NULL')
        after = SIMPLE.replace('WHERE Status = 1', 'WHERE Status=1 OR (Status=2 AND Id IS NOT NULL)')
        differences = verifier.differences(verifier.parse_schema(before), verifier.parse_schema(after))
        self.assertEqual([item['kind'] for item in differences], ['indexes'])

    def test_named_defaults_and_identity_seed_are_significant(self):
        changed = SIMPLE.replace('DF_Sample_Status', 'DF_Renamed')
        self.assertTrue(verifier.differences(verifier.parse_schema(SIMPLE), verifier.parse_schema(changed)))
        identity = SIMPLE.replace('Id UNIQUEIDENTIFIER NOT NULL', 'Id INT IDENTITY(2,3) NOT NULL')
        self.assertEqual(verifier.parse_schema(identity)['columns'][('Sample', 'Id')]['identity'], (2, 3))

    def test_only_safe_default_cast_equivalence(self):
        self.assertEqual(verifier.default_expression('((1))', 'bit'), verifier.default_expression('CAST(1 AS bit)', 'bit'))
        with self.assertRaises(verifier.UnsupportedSql):
            verifier.default_expression('CAST(2 AS bit)', 'bit')
        with self.assertRaises(verifier.UnsupportedSql):
            verifier.default_expression('CAST(1 AS bit)', 'int')

    def test_literal_comma_and_comment_markers_preserved(self):
        sql = SIMPLE.replace('Name NVARCHAR(20) NOT NULL', "Name NVARCHAR(20) NOT NULL CONSTRAINT DF_Sample_Name DEFAULT (N'a,--b/*c*/')")
        self.assertEqual(verifier.parse_schema(sql)['columns'][('Sample', 'Name')]['default'],
                         ('DF_Sample_Name', "N'a,--b/*c*/'"))

    def test_unsupported_schema_syntax_fails_closed(self):
        unsupported = [
            SIMPLE.replace('dbo.Sample', 'other.Sample'),
            SIMPLE + 'CREATE TABLE Extra (Id INT NOT NULL);',
            SIMPLE + 'ALTER TABLE dbo.Sample ADD Extra INT;',
            SIMPLE + 'CREATE SEQUENCE dbo.NewSequence AS INT START WITH 1;',
            SIMPLE + 'CREATE FUNCTION dbo.NewFunction() RETURNS INT AS BEGIN RETURN 1; END;',
            SIMPLE + 'CREATE PROCEDURE dbo.NewProcedure AS SELECT 1;',
            SIMPLE.replace('ON dbo.Sample(Name)', 'ON dbo.Sample(Name DESC)'),
            SIMPLE.replace('WHERE Status = 1', 'INCLUDE (Id)'),
            SIMPLE.replace('Name NVARCHAR(20) NOT NULL', 'Name NVARCHAR(20) COLLATE Latin1_General_CS_AS NOT NULL'),
            SIMPLE.replace('CONSTRAINT PK_Sample PRIMARY KEY (Id)', 'CONSTRAINT UQ_Sample UNIQUE (Id)'),
        ]
        for sql in unsupported:
            with self.subTest(sql=sql), self.assertRaises(verifier.UnsupportedSql):
                verifier.parse_schema(sql)

    def test_column_collation_drift_rejected(self):
        catalog = {
            'database': [{'collation_name': 'SQL_Latin1_General_CP1_CI_AS'}],
            'tables': [{'schema': 'dbo', 'table': 'Sample', 'temporal_type': 0, 'is_memory_optimized': False}],
            'columns': [{'schema': 'dbo', 'table': 'Sample', 'column': 'Name', 'type': 'nvarchar',
                         'max_length': 40, 'is_nullable': False, 'is_identity': False, 'is_computed': False,
                         'collation_name': 'SQL_Latin1_General_CP1_CI_AS'}],
            'foreignKeys': [], 'checks': [], 'indexes': [], 'triggers': [],
        }
        self.assertEqual(verifier.catalog_schema(catalog)['columns'][('Sample', 'Name')]['type'], 'nvarchar(20)')
        catalog['columns'][0]['collation_name'] = 'Latin1_General_CS_AS'
        with self.assertRaisesRegex(verifier.UnsupportedSql, 'collation'):
            verifier.catalog_schema(catalog)

    def test_optional_ef_history_is_ignored_but_other_tables_are_not(self):
        base = {
            'database': [{'collation_name': 'SQL_Latin1_General_CP1_CI_AS'}],
            'tables': [{'schema': 'dbo', 'table': 'Sample',
                        'temporal_type': 0, 'is_memory_optimized': False}],
            'columns': [], 'foreignKeys': [], 'checks': [], 'indexes': [], 'triggers': [],
        }
        expected = verifier.catalog_schema(base)

        # EF Core can create its standard history table after adopting the schema.
        with_history = {key: list(value) for key, value in base.items()}
        with_history['tables'].append({
            'schema': 'dbo', 'table': '__EFMigrationsHistory',
            'temporal_type': 0, 'is_memory_optimized': False,
        })
        with_history['columns'].append({
            'schema': 'dbo', 'table': '__EFMigrationsHistory',
            'column': 'MigrationId', 'type': 'nvarchar',
        })
        with_history['indexes'].append({
            'schema': 'dbo', 'table': '__EFMigrationsHistory',
            'name': 'PK___EFMigrationsHistory',
        })
        self.assertEqual(expected, verifier.catalog_schema(with_history))

        with_history['tables'].append({
            'schema': 'dbo', 'table': 'Unexpected',
            'temporal_type': 0, 'is_memory_optimized': False,
        })
        self.assertTrue(verifier.differences(expected, verifier.catalog_schema(with_history)))

        with_history['tables'][-1]['schema'] = 'other'
        with self.assertRaisesRegex(verifier.UnsupportedSql, 'Unsupported table metadata'):
            verifier.catalog_schema(with_history)

    def test_cli_exit_codes_for_match_drift_and_unsupported(self):
        script = Path(verifier.__file__)
        with tempfile.TemporaryDirectory() as directory:
            source, generated = Path(directory) / 'source.sql', Path(directory) / 'generated.sql'
            source.write_text(SIMPLE)
            for sql, expected in ((SIMPLE, 0), (SIMPLE.replace('NVARCHAR(20)', 'NVARCHAR(40)'), 1),
                                  (SIMPLE + 'DROP TABLE dbo.Sample;', 2)):
                generated.write_text(sql)
                result = subprocess.run([sys.executable, str(script), '--source', str(source), '--generated', str(generated)], capture_output=True, text=True)
                self.assertEqual(result.returncode, expected, result.stdout + result.stderr)


if __name__ == '__main__':
    unittest.main()
