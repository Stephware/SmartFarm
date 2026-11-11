#!/usr/bin/env bash
# Read-only SQL Server backup verification. Path is on the SQL Server host, not on this computer.
# Usage: SMARTFARM_SQL_SERVER=host bash scripts/verify_sql_backup.sh /path/on/server/SmartFarm.bak
set -euo pipefail
if [[ $# -ne 1 || "$1" == "-h" || "$1" == "--help" ]]; then
    echo "Usage: SMARTFARM_SQL_SERVER=host bash scripts/verify_sql_backup.sh SQL_SERVER_BACKUP_PATH"
    exit 2
fi
command -v sqlcmd >/dev/null || { echo "Missing sqlcmd client." >&2; exit 2; }
if [[ -z "${SMARTFARM_SQL_SERVER:-}" ]]; then
    echo "Set SMARTFARM_SQL_SERVER. Backup path must exist on that SQL Server host." >&2; exit 2
fi
backup_path=$1
if [[ "$backup_path" == *$'\n'* || "$backup_path" == *$'\r'* || -z "$backup_path" ]]; then
    echo "Invalid backup path." >&2; exit 2
fi
# SQL Server Unicode string literals escape a single quote by doubling it.
escaped_path=$(printf '%s' "$backup_path" | sed "s/'/''/g")
cmd=(sqlcmd -S "$SMARTFARM_SQL_SERVER" -d master -b -r 1)
if [[ -n "${SMARTFARM_SQL_USER:-}" ]]; then
    cmd+=(-U "$SMARTFARM_SQL_USER")
fi
if [[ "${SMARTFARM_SQL_TRUST_CERT:-0}" == "1" ]]; then
    cmd+=(-C)
fi
# For SQL authentication, export SQLCMDPASSWORD (never provide credentials on the command line).
echo "Checking existing SQL Server backup for readable structure and checksums (no restore performed)..."
"${cmd[@]}" -Q "RESTORE VERIFYONLY FROM DISK = N'$escaped_path' WITH CHECKSUM;"
echo "PASS: SQL Server RESTORE VERIFYONLY completed."
echo "NOT VERIFIED: data can be recovered and used. A disposable full restore is still required."
