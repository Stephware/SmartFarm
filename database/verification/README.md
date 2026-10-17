# Read-only schema verification

`verify_schema.py` compares the reviewed SQL-first contract with EF Core's
`GenerateCreateScript()` output, a live SQL Server metadata export, or both.
It uses Python 3's standard library, reads local files, and never connects to
the database or executes SQL. Keep generated SQL and catalog exports outside
the repository.

From the repository root:

```sh
python3 database/verification/verify_schema.py \
  --source database/02_CreateSchema.sql \
  --generated /path/to/ef-generated.sql \
  --live-schema /path/to/live-schema.json

python3 -m unittest discover -s database/verification -v
```

Supply at least one of `--generated` and `--live-schema`. To obtain the live
metadata file, run `read_catalog.sql` through your existing SQL connection to
`SmartFarmDB` and export its single JSON result as UTF-8. This query reads
system catalogs only; it does not read application records or credentials.
The account must be able to see the complete database schema. Truncated or
incomplete metadata is unsuitable for baseline approval.

The report lists differences by object. Exit codes are `0` for matching
contracts, `1` for schema differences, and `2` for invalid input or unsupported
syntax. A successful report for the current contract has 23 tables, 193
columns, 23 primary keys, 39 foreign keys, 36 checks, and 45 secondary indexes.

Comparison includes column types, lengths, precision/scale, nullability,
identity seed/increment, named defaults, named keys and foreign keys, deletion
rules, unique indexes, key order, filters, and check predicates. Live metadata
also verifies trusted/enabled constraints, index storage flags, absence of
triggers, and column collations inherited from the database default.

Predicate comparison preserves parentheses and operator precedence. It
normalizes SQL Server's `IN` and `BETWEEN` expansions and associative ordering
of `AND`/`OR`. It never compares checks by deleting parentheses. Supported
normalizations also include `rowversion`/`timestamp`, an omitted `ON DELETE
NO ACTION`, and exact constant casts such as `CAST(1 AS bit)` for a bit column.
Physical column order is ignored because it does not affect EF's explicit
column mappings; primary/index key order remains significant.

This is a deliberately scoped parser for the SmartFarm CREATE contract,
rather than a general SQL interpreter. Unsupported schema definitions,
column clauses, default functions, or predicates fail verification. It rejects
additional CREATE object kinds, ALTER/DROP scripts, non-dbo objects, included
or descending index keys, computed columns, temporal tables, and memory
optimized tables. Extend the parser and its regression tests before adopting
such schema features. SQL batch/transaction wrappers are not executed or
semantically interpreted.

Matching files are evidence for model review; they do not authorize database
changes or migration-history adoption. Re-export current metadata and obtain
explicit approval before applying any migration or writing migration history.
