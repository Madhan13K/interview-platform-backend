-- Change refresh_tokens.token column from varchar(255) to TEXT to support longer JWT tokens with jti claim
ALTER TABLE refresh_tokens ALTER COLUMN token TYPE TEXT;

