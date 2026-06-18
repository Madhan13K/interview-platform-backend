-- Add auth_provider column to track how the user was created
ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

-- Drop authentication_sessions table (no longer needed - using stateless JWT)
DROP TABLE IF EXISTS authentication_sessions;

