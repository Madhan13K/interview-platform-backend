-- V22: Data Encryption at Rest - Alter columns to support encrypted values
-- Encrypted fields are stored as base64 strings (prefixed with "ENC:")
-- so NUMERIC/DECIMAL columns need to become TEXT.

-- Alter salary columns in job_positions to TEXT for encrypted storage
ALTER TABLE job_positions ALTER COLUMN salary_min TYPE TEXT USING salary_min::TEXT;
ALTER TABLE job_positions ALTER COLUMN salary_max TYPE TEXT USING salary_max::TEXT;

-- Note: phone_number in users table is already VARCHAR which can hold encrypted values.
-- Encrypted base64 strings are typically ~2x the length of the original + 44 chars overhead.
-- Ensure phone_number and contact_number columns are wide enough.
ALTER TABLE users ALTER COLUMN phone_number TYPE TEXT;

-- Ensure user_profiles columns can hold encrypted values
-- (contactNumber, linkedinUrl, githubUrl may need wider columns)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'user_profiles' AND column_name = 'contact_number') THEN
        ALTER TABLE user_profiles ALTER COLUMN contact_number TYPE TEXT;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'user_profiles' AND column_name = 'linkedin_url') THEN
        ALTER TABLE user_profiles ALTER COLUMN linkedin_url TYPE TEXT;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'user_profiles' AND column_name = 'github_url') THEN
        ALTER TABLE user_profiles ALTER COLUMN github_url TYPE TEXT;
    END IF;
END $$;
