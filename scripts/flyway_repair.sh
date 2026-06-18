#!/bin/bash
# Flyway Repair Script
# Run this after deployment if flyway_schema_history has checksum mismatches
# (e.g., V8 was manually inserted without correct checksum)
#
# Usage: ./scripts/flyway_repair.sh
#
# This requires the flyway CLI or can be run via Maven:

cd "$(dirname "$0")/.."

echo "Running Flyway repair..."
./mvnw flyway:repair \
  -Dflyway.url="${DB_URL:-jdbc:postgresql://localhost:5433/interview_platform}" \
  -Dflyway.user="${DB_USERNAME:-admin}" \
  -Dflyway.password="${DB_PASSWORD:-postgres}"

echo "Flyway repair complete. Checksums reconciled."

