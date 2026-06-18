-- V30: Source Effectiveness - Candidate source tracking and ROI
CREATE TABLE candidate_sources (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id      UUID NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    source              VARCHAR(50) NOT NULL,
    source_campaign     VARCHAR(200),
    cost_per_click      DECIMAL(10, 2),
    total_spend         DECIMAL(12, 2),
    attributed_at       TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_candidate_sources_application ON candidate_sources(application_id);
CREATE INDEX idx_candidate_sources_source ON candidate_sources(source);
CREATE INDEX idx_candidate_sources_campaign ON candidate_sources(source_campaign);
