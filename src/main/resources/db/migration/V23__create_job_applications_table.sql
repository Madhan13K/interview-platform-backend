-- V23: Candidate Portal / Job Board - Job applications and status tracking
CREATE TABLE job_applications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_position_id     UUID NOT NULL REFERENCES job_positions(id) ON DELETE CASCADE,
    candidate_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status              VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    cover_letter        TEXT,
    resume_url          VARCHAR(500),
    source              VARCHAR(30) DEFAULT 'PORTAL',
    referral_code       VARCHAR(100),
    notes               TEXT,
    applied_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    reviewed_at         TIMESTAMP WITH TIME ZONE,
    status_updated_at   TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_application_position_candidate UNIQUE (job_position_id, candidate_id)
);

CREATE INDEX idx_job_applications_candidate ON job_applications(candidate_id);
CREATE INDEX idx_job_applications_position ON job_applications(job_position_id);
CREATE INDEX idx_job_applications_status ON job_applications(status);
CREATE INDEX idx_job_applications_applied ON job_applications(applied_at DESC);
CREATE INDEX idx_job_applications_source ON job_applications(source);
