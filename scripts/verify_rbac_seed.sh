#!/usr/bin/env zsh
set -euo pipefail

DB_URL="${DATABASE_URL:-postgresql://postgres:postgres@localhost:5432/interview_platform}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SQL_FILE="$SCRIPT_DIR/verify_rbac_seed.sql"

echo "Running RBAC seed verification against: $DB_URL"
psql "$DB_URL" -f "$SQL_FILE"

