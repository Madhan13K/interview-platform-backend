-- V28: Referral Program - Employee referral tracking and bonuses
CREATE TABLE referrals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referrer_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    candidate_email VARCHAR(255) NOT NULL,
    candidate_name  VARCHAR(300) NOT NULL,
    job_position_id UUID REFERENCES job_positions(id) ON DELETE SET NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    referral_code   VARCHAR(50) NOT NULL UNIQUE,
    bonus_amount    DECIMAL(12, 2),
    bonus_paid_at   TIMESTAMP WITH TIME ZONE,
    notes           TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_referrals_referrer ON referrals(referrer_id);
CREATE INDEX idx_referrals_code ON referrals(referral_code);
CREATE INDEX idx_referrals_candidate ON referrals(candidate_email);
CREATE INDEX idx_referrals_status ON referrals(status);
CREATE INDEX idx_referrals_position ON referrals(job_position_id);
