#!/bin/bash
# =============================================================================
# Database User Separation Script
# =============================================================================
# Creates separate users for DDL (migrations) and DML (application) operations.
# Run this ONCE on the PostgreSQL instance before deploying.
#
# - ddl_admin: Has CREATE, ALTER, DROP privileges (used by Flyway migrations)
# - app_user:  Has SELECT, INSERT, UPDATE, DELETE only (used by application)
#
# Usage: PGPASSWORD=postgres psql -h localhost -p 5433 -U admin -d interview_platform -f scripts/db/setup-users.sql
# =============================================================================

-- Create roles
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'ddl_admin') THEN
        CREATE ROLE ddl_admin WITH LOGIN PASSWORD 'ddl_secure_password';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_user') THEN
        CREATE ROLE app_user WITH LOGIN PASSWORD 'app_secure_password';
    END IF;
END $$;

-- =============================================================================
-- DDL Admin: Full schema management (used by Flyway)
-- =============================================================================
GRANT CONNECT ON DATABASE interview_platform TO ddl_admin;
GRANT USAGE ON SCHEMA public TO ddl_admin;
GRANT CREATE ON SCHEMA public TO ddl_admin;

-- DDL admin gets full table/sequence privileges (needs to create, alter, drop)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ddl_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ddl_admin;

-- Ensure future tables/sequences also get these privileges
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO ddl_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO ddl_admin;

-- =============================================================================
-- App User: DML only (SELECT, INSERT, UPDATE, DELETE)
-- =============================================================================
GRANT CONNECT ON DATABASE interview_platform TO app_user;
GRANT USAGE ON SCHEMA public TO app_user;

-- App user gets DML-only access
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;

-- Ensure future tables/sequences also get DML privileges for app_user
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO app_user;

-- Explicitly DENY DDL operations to app_user (PostgreSQL does this by default for non-owners)
-- app_user cannot: CREATE TABLE, ALTER TABLE, DROP TABLE, TRUNCATE

-- =============================================================================
-- Verify permissions
-- =============================================================================
-- You can verify with:
--   \du ddl_admin
--   \du app_user
--   \dp (shows table permissions)
