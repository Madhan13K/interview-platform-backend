-- V29: DEI/Diversity Analytics - Opt-in demographic tracking
CREATE TABLE demographic_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    gender              VARCHAR(30),
    ethnicity           VARCHAR(50),
    veteran_status      BOOLEAN,
    disability_status   BOOLEAN,
    age_range           VARCHAR(30),
    consent_given       BOOLEAN NOT NULL DEFAULT FALSE,
    consent_given_at    TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_demographic_profiles_user ON demographic_profiles(user_id);
CREATE INDEX idx_demographic_profiles_consent ON demographic_profiles(consent_given) WHERE consent_given = TRUE;
-- Note: No index on individual demographic fields to prevent targeted queries
