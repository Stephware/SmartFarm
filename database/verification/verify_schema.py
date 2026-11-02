#!/usr/bin/env python3
"""Read-only comparison of the supported SmartFarm SQL Server schema contract."""

import argparse
from collections import defaultdict
import json
from pathlib import Path
import re
import sys


class UnsupportedSql(ValueError):
    """Input cannot be compared safely by this deliberately scoped parser."""


def require(condition, message):
    if not condition:
        raise UnsupportedSql(message)


def clean_sql(sql):
    """Remove comments/identifier brackets while preserving quoted literal contents."""
    token = re.compile(r"N?'(?:''|[^'])*'|\[(?:]]|[^]])*\]|/\*.*?\*/|--[^\n]*", re.S)
    def replace(match):
        value = match[0]
        if value.startswith(('--', '/*')):
            return ' '
        if value.startswith('['):
            identifier = value[1:-1].replace(']]', ']')
            require(re.fullmatch(r'\w+', identifier), 'Unsupported identifier: ' + value)
            return identifier
        return value
    return token.sub(replace, sql).lstrip('\ufeff')


def balanced(text, start):
    require(text[start] == '(', 'Expected opening parenthesis')
    depth = 0
    quoted = False
    i = start
    while i < len(text):
        char = text[i]
        if char == "'":
            if quoted and i + 1 < len(text) and text[i + 1] == "'":
                i += 2
                continue
            quoted = not quoted
        if not quoted:
            if char == '(':
                depth += 1
            elif char == ')':
                depth -= 1
                if depth == 0:
                    return text[start + 1:i], i + 1
        i += 1
    raise UnsupportedSql('Unbalanced SQL parentheses or literal')


def split_parts(text):
    parts = []
    start = i = 0
    while i < len(text):
        if text[i] == '(':
            _, i = balanced(text, i)
            continue
        if text[i] == "'":
            match = re.match(r"'(?:''|[^'])*'", text[i:])
            require(match, 'Unclosed string literal')
            i += match.end()
            continue
        if text[i] == ',':
            parts.append(text[start:i].strip())
            start = i + 1
        i += 1
    parts.append(text[start:].strip())
    return parts


def combine(operator, *arguments):
    flattened = []
    for argument in arguments:
        flattened.extend(argument[1:] if argument[0] == operator else [argument])
    return flattened[0] if len(flattened) == 1 else (operator, *sorted(flattened, key=repr))


class Predicate:
    """Small predicate AST; grouping is preserved, IN/BETWEEN are expanded."""

    def __init__(self, sql):
        text = clean_sql(sql)
        self.tokens = []
        position = 0
        pattern = re.compile(r"\s*(N?'(?:''|[^'])*'|<>|>=|<=|[A-Za-z_]\w*|-?\d+(?:\.\d+)?|[(),=<>])")
        while position < len(text):
            if not text[position:].strip():
                break
            match = pattern.match(text, position)
            require(match, 'Unsupported predicate syntax: ' + text[position:])
            token = match[1]
            self.tokens.append(token.removeprefix('N') if "'" in token else token.lower())
            position = match.end()
        self.index = 0

    def peek(self):
        return self.tokens[self.index] if self.index < len(self.tokens) else None

    def pop(self, expected=None):
        value = self.peek()
        require(value is not None and (expected is None or value == expected),
                f'Expected {expected or "token"}; received {value}')
        self.index += 1
        return value

    def scalar(self):
        if self.peek() == '(':
            self.pop()
            value = self.scalar()
            self.pop(')')
            return value
        value = self.pop()
        require(re.fullmatch(r"'(?:''|[^'])*'|[a-z_]\w*|-?\d+(?:\.\d+)?", value), 'Unsupported scalar')
        return ('atom', value)

    def predicate(self):
        if self.peek() == 'not':
            self.pop()
            return ('not', self.predicate())
        if self.peek() == '(':
            # Parentheses can wrap a scalar operand or an entire predicate.
            start = self.index
            try:
                self.pop()
                value = self.or_expression()
                self.pop(')')
                if self.peek() not in ('=', '<>', '<', '>', '<=', '>=', 'is', 'in', 'not', 'between'):
                    return value
            except UnsupportedSql:
                pass
            self.index = start
        left = self.scalar()
        operator = self.pop()
        negate = operator == 'not'
        if negate:
            operator = self.pop()
            require(operator == 'in', 'Only NOT IN is supported after a scalar')
        if operator == 'is':
            not_null = self.peek() == 'not'
            if not_null:
                self.pop()
            self.pop('null')
            return ('isnotnull' if not_null else 'isnull', left)
        if operator == 'between':
            lower = self.scalar()
            self.pop('and')
            upper = self.scalar()
            value = combine('and', ('>=', left, lower), ('<=', left, upper))
        elif operator == 'in':
            self.pop('(')
            values = [self.scalar()]
            while self.peek() == ',':
                self.pop()
                values.append(self.scalar())
            self.pop(')')
            value = combine('or', *(('=', left, item) for item in values))
        else:
            require(operator in ('=', '<>', '<', '>', '<=', '>='), 'Unsupported comparison')
            value = (operator, left, self.scalar())
        return ('not', value) if negate else value

    def and_expression(self):
        values = [self.predicate()]
        while self.peek() == 'and':
            self.pop()
            values.append(self.predicate())
        return combine('and', *values)

    def or_expression(self):
        values = [self.and_expression()]
        while self.peek() == 'or':
            self.pop()
            values.append(self.and_expression())
        return combine('or', *values)

    def parse(self):
        result = self.or_expression()
        require(self.index == len(self.tokens), 'Trailing predicate tokens')
        return result


def predicate(sql):
    return Predicate(sql).parse()


def default_expression(sql, column_type):
    text = sql.strip()
    while text.startswith('('):
        body, end = balanced(text, 0)
        if end != len(text):
            break
        text = body.strip()
    if re.fullmatch(r"N?'(?:''|[^'])*'", text):
        return text
    text = text.lower()
    cast = re.fullmatch(r'cast\s*\(\s*(-?\d+(?:\.\d+)?)\s+as\s+(bit|decimal\(\d+,\s*\d+\))\s*\)', text)
    if cast:
        target = re.sub(r'\s', '', cast[2])
        require(target == column_type, 'Default CAST target must match column type')
        require(target != 'bit' or cast[1] in ('0', '1'), 'Unsupported bit conversion')
        return cast[1]
    require(re.fullmatch(r'-?\d+(?:\.\d+)?|sysutcdatetime\(\)|newid\(\)', text),
            'Unsupported default expression: ' + text)
    return text


def add(mapping, key, value):
    require(key not in mapping, f'Duplicate schema object: {key}')
    mapping[key] = value


def keys(sql):
    values = [item.strip() for item in sql.split(',')]
    require(all(re.fullmatch(r'\w+', value) for value in values), 'Unsupported key expression/order')
    return tuple(values)


def empty_schema():
    return {name: {} for name in ('tables', 'columns', 'primary_keys', 'foreign_keys', 'checks', 'indexes')}


def parse_schema(sql):
    text = clean_sql(sql)
    scan_text = re.sub(r"N?'(?:''|[^'])*'", lambda match: ' ' * len(match[0]), text)
    # This is a CREATE-contract comparator, not an interpreter for ALTER scripts.
    require(not re.search(r'\b(?:ALTER|DROP)\s+\w+|\bCREATE\s+(?!(?:TABLE|(?:UNIQUE\s+)?INDEX)\b)\w+', scan_text, re.I),
            'ALTER/DROP and additional schema object kinds are unsupported')
    schema = empty_schema()
    matches = list(re.finditer(r'CREATE TABLE (\w+)\.(\w+)\s*\(', scan_text, re.I))
    require(len(matches) == len(re.findall(r'\bCREATE\s+TABLE\b', scan_text, re.I)), 'Unsupported CREATE TABLE syntax')
    require(matches, 'No CREATE TABLE definitions found')
    for match in matches:
        namespace, table = match.groups()
        require(namespace == 'dbo', 'Only the dbo schema is supported')
        body, end = balanced(text, match.end() - 1)
        require(text[end:].lstrip().startswith(';'), 'Unsupported table suffix')
        add(schema['tables'], table, True)
        for part in split_parts(body):
            constraint = re.fullmatch(r'CONSTRAINT (\w+) (.*)', part, re.S)
            if constraint:
                name, definition = constraint.groups()
                key = (table, name)
                primary = re.fullmatch(r'PRIMARY KEY\s*\(([^)]+)\)', definition)
                foreign = re.fullmatch(r'FOREIGN KEY\s*\((\w+)\)\s*REFERENCES (\w+)\.(\w+)\s*\((\w+)\)(?: ON DELETE (CASCADE|NO ACTION))?', definition)
                if primary:
                    add(schema['primary_keys'], key, keys(primary[1]))
                elif foreign:
                    column, target_schema, target_table, target_column, deletion = foreign.groups()
                    require(target_schema == 'dbo', 'Unsupported referenced schema')
                    add(schema['foreign_keys'], key, (column, target_table, target_column, deletion or 'NO ACTION'))
                elif definition.startswith('CHECK'):
                    start = definition.find('(')
                    require(start >= 0 and definition[:start].strip() == 'CHECK', 'Unsupported CHECK')
                    expression, end = balanced(definition, start)
                    require(not definition[end:].strip(), 'Unsupported CHECK suffix')
                    add(schema['checks'], key, predicate(expression))
                else:
                    raise UnsupportedSql('Unsupported table constraint: ' + definition)
                continue
            column = re.fullmatch(r'(\w+)\s+(\w+)(?:\(([^)]+)\))?\s+(.*)', part, re.S)
            require(column, 'Unsupported column: ' + part)
            name, kind, parameters, tail = column.groups()
            kind = kind.lower()
            kind = 'rowversion' if kind == 'timestamp' else kind
            require(kind in ('uniqueidentifier', 'nvarchar', 'bit', 'int', 'datetimeoffset', 'datetime2', 'decimal', 'rowversion', 'tinyint', 'smallint', 'date', 'bigint', 'varbinary'), 'Unsupported type: ' + kind)
            column_type = kind + (f'({parameters.lower().replace(" ", "")})' if parameters else '')
            null = re.match(r'(NOT NULL|NULL)\b\s*', tail)
            # Source places IDENTITY before nullability; EF places it afterwards.
            before = re.match(r'IDENTITY(?:\((\d+),\s*(\d+)\))?\s+', tail)
            identity = None
            if before:
                identity = (int(before[1] or 1), int(before[2] or 1))
                tail = tail[before.end():]
                null = re.match(r'(NOT NULL|NULL)\b\s*', tail)
            require(null, 'Explicit column nullability required: ' + part)
            nullable = null[1] == 'NULL'
            tail = tail[null.end():]
            after = re.match(r'IDENTITY(?:\((\d+),\s*(\d+)\))?\s*', tail)
            if after:
                require(identity is None, 'Duplicate IDENTITY')
                identity = (int(after[1] or 1), int(after[2] or 1))
                tail = tail[after.end():]
            default = None
            if tail:
                value = re.fullmatch(r'CONSTRAINT (\w+) DEFAULT (.*)', tail, re.S)
                require(value, 'Unsupported column suffix: ' + tail)
                default = (value[1], default_expression(value[2], column_type))
            add(schema['columns'], (table, name), {'type': column_type, 'nullable': nullable, 'identity': identity, 'default': default})
    index_matches = list(re.finditer(r'CREATE (UNIQUE )?INDEX (\w+) ON (\w+)\.(\w+)\s*\(([^)]+)\)(?: WHERE ([^;]+))?;', text, re.I))
    require(len(index_matches) == len(re.findall(r'\bCREATE\s+(?:UNIQUE\s+)?INDEX\b', scan_text, re.I)), 'Unsupported CREATE INDEX syntax')
    for match in index_matches:
        unique, name, namespace, table, columns, where = match.groups()
        require(namespace == 'dbo' and table in schema['tables'], 'Unknown index table/schema')
        add(schema['indexes'], (table, name), {'unique': bool(unique), 'keys': keys(columns), 'filter': predicate(where) if where else None})
    return schema


def catalog_schema(data):
    """Accept metadata JSON captured by the documented read-only catalog query."""
    schema = empty_schema()
    require(not data['triggers'], 'Unexpected triggers')
    require(len(data['database']) == 1, 'Exactly one database metadata row required')
    database_collation = data['database'][0]['collation_name']

    # An adopted EF Core baseline may create its standard history table.
    # Only ignore metadata belonging to that exact dbo table; all other drift remains visible.
    def app_rows(group):
        return [row for row in data[group]
                if not (row['schema'] == 'dbo' and row['table'] == '__EFMigrationsHistory')]

    for table in app_rows('tables'):
        require(table['schema'] == 'dbo' and not table['temporal_type'] and not table['is_memory_optimized'], 'Unsupported table metadata')
        add(schema['tables'], table['table'], True)
    for column in app_rows('columns'):
        require(column['schema'] == 'dbo' and not column['is_computed'], 'Unsupported column metadata')
        if column.get('collation_name'):
            require(column['collation_name'] == database_collation, 'Column collation differs from database default')
        require(bool(column.get('default_name')) == bool(column.get('default_sql')), 'Incomplete default constraint metadata')
        kind = column['type']
        kind = 'rowversion' if kind == 'timestamp' else kind
        column_type = kind
        if kind in ('nvarchar', 'varbinary'):
            length = column['max_length']
            if kind == 'nvarchar' and length != -1:
                require(length % 2 == 0, 'Invalid nvarchar byte length')
                length //= 2
            column_type += '(' + ('max' if length == -1 else str(length)) + ')'
        elif kind == 'decimal':
            column_type += f"({column['precision']},{column['scale']})"
        elif kind in ('datetimeoffset', 'datetime2'):
            column_type += f"({column['scale']})"
        default = None
        if column.get('default_name'):
            default = (column['default_name'], default_expression(column['default_sql'], column_type))
        identity = (column['identity_seed'], column['identity_increment']) if column['is_identity'] else None
        add(schema['columns'], (column['table'], column['column']), {'type': column_type, 'nullable': column['is_nullable'], 'identity': identity, 'default': default})
    for foreign in app_rows('foreignKeys'):
        require(foreign['schema'] == foreign['referenced_schema'] == 'dbo' and not foreign['is_disabled'] and not foreign['is_not_trusted'] and foreign['update_referential_action_desc'] == 'NO_ACTION', 'Unsupported or untrusted FK metadata')
        require(foreign['constraint_column_id'] == 1, 'Composite FK unsupported')
        add(schema['foreign_keys'], (foreign['table'], foreign['name']), (foreign['column'], foreign['referenced_table'], foreign['referenced_column'], foreign['delete_referential_action_desc'].replace('_', ' ')))
    for check in app_rows('checks'):
        require(check['schema'] == 'dbo' and not check['is_disabled'] and not check['is_not_trusted'], 'Unsupported or untrusted CHECK metadata')
        add(schema['checks'], (check['table'], check['name']), predicate(check['definition']))
    indexes = defaultdict(list)
    for index in app_rows('indexes'):
        require(index['schema'] == 'dbo' and not index['is_disabled'] and not index['is_included_column'] and not index['is_descending_key'] and not index['is_unique_constraint'], 'Unsupported index metadata')
        indexes[(index['table'], index['name'])].append(index)
    for key, rows in indexes.items():
        row = rows[0]
        columns = tuple(item['column'] for item in sorted(rows, key=lambda item: item['key_ordinal']))
        if row['is_primary_key']:
            require(row['type_desc'] == 'CLUSTERED' and row['is_unique'], 'Unsupported primary key metadata')
            add(schema['primary_keys'], key, columns)
        else:
            require(row['type_desc'] == 'NONCLUSTERED', 'Unsupported index storage type')
            add(schema['indexes'], key, {'unique': row['is_unique'], 'keys': columns, 'filter': predicate(row['filter_definition']) if row.get('filter_definition') else None})
    return schema


def differences(expected, actual):
    result = []
    for kind, objects in expected.items():
        observed = actual[kind]
        for key in sorted(objects.keys() | observed.keys()):
            if objects.get(key) != observed.get(key):
                result.append({'kind': kind, 'object': key, 'expected': objects.get(key), 'actual': observed.get(key)})
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True, help='Reviewed SQL-first CREATE schema')
    parser.add_argument('--generated', type=Path, help='EF GenerateCreateScript output (never executed)')
    parser.add_argument('--live-schema', type=Path, help='Read-only SQL Server catalog metadata JSON')
    args = parser.parse_args()
    if not args.generated and not args.live_schema:
        parser.error('Provide --generated and/or --live-schema')
    try:
        expected = parse_schema(args.source.read_text(encoding='utf-8-sig'))
        report = {'source_counts': {name: len(items) for name, items in expected.items()}, 'comparisons': {}}
        if args.generated:
            report['comparisons']['generated'] = differences(expected, parse_schema(args.generated.read_text(encoding='utf-8-sig')))
        if args.live_schema:
            report['comparisons']['live'] = differences(expected, catalog_schema(json.loads(args.live_schema.read_text(encoding='utf-8-sig'))))
        print(json.dumps(report, indent=2))
        return 1 if any(report['comparisons'].values()) else 0
    except (UnsupportedSql, OSError, KeyError, TypeError, ValueError) as error:
        print(f'Schema verification failed: {error}', file=sys.stderr)
        return 2


if __name__ == '__main__':
    sys.exit(main())
