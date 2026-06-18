-- Add token_family column for refresh token rotation replay detection
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS token_family VARCHAR(255);

-- Create index for fast family-based queries
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_family ON refresh_tokens(token_family);

