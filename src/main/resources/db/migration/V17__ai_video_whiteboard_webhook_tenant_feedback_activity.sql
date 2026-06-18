-- ============================================================
-- V17: AI, Video, Whiteboard, Webhooks, Multi-Tenant,
--      Candidate Feedback (Reverse), Activity Feed
-- ============================================================

-- =========================
-- 1. Multi-Tenant Support
-- =========================
CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    domain VARCHAR(200),
    logo_url VARCHAR(500),
    plan VARCHAR(50) DEFAULT 'FREE',
    max_users INTEGER DEFAULT 50,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS organization_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(organization_id, user_id)
);

-- Add organization_id to users for default org
ALTER TABLE users ADD COLUMN IF NOT EXISTS organization_id UUID REFERENCES organizations(id);

-- =========================
-- 2. AI Suggestions
-- =========================
CREATE TABLE IF NOT EXISTS ai_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id),
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(50) NOT NULL,
    input_context TEXT,
    output_content TEXT NOT NULL,
    model VARCHAR(100),
    tokens_used INTEGER,
    confidence_score DOUBLE PRECISION,
    status VARCHAR(30) DEFAULT 'GENERATED',
    interview_id UUID REFERENCES interviews(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_suggestions_user ON ai_suggestions(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_suggestions_type ON ai_suggestions(type);
CREATE INDEX IF NOT EXISTS idx_ai_suggestions_interview ON ai_suggestions(interview_id);

-- =========================
-- 3. Video Recordings
-- =========================
CREATE TABLE IF NOT EXISTS video_recordings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id),
    interview_id UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    recorded_by UUID NOT NULL REFERENCES users(id),
    file_name VARCHAR(300) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    s3_bucket VARCHAR(200) NOT NULL,
    file_size_bytes BIGINT,
    duration_seconds INTEGER,
    mime_type VARCHAR(100) DEFAULT 'video/webm',
    status VARCHAR(30) DEFAULT 'PROCESSING',
    thumbnail_url VARCHAR(500),
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_video_recordings_interview ON video_recordings(interview_id);
CREATE INDEX IF NOT EXISTS idx_video_recordings_status ON video_recordings(status);

-- =========================
-- 4. Whiteboard Sessions
-- =========================
CREATE TABLE IF NOT EXISTS whiteboard_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id),
    interview_id UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id),
    title VARCHAR(200) DEFAULT 'Untitled Whiteboard',
    snapshot_data TEXT,
    thumbnail_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS whiteboard_strokes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES whiteboard_sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    stroke_data JSONB NOT NULL,
    tool VARCHAR(30) NOT NULL DEFAULT 'PEN',
    color VARCHAR(20) DEFAULT '#000000',
    stroke_width DOUBLE PRECISION DEFAULT 2.0,
    sequence_number INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_whiteboard_sessions_interview ON whiteboard_sessions(interview_id);
CREATE INDEX IF NOT EXISTS idx_whiteboard_strokes_session ON whiteboard_strokes(session_id);

-- =========================
-- 5. Webhook Endpoints & Deliveries
-- =========================
CREATE TABLE IF NOT EXISTS webhook_endpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id),
    user_id UUID NOT NULL REFERENCES users(id),
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(200) NOT NULL,
    description VARCHAR(300),
    events TEXT[] NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS webhook_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    endpoint_id UUID NOT NULL REFERENCES webhook_endpoints(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    response_status INTEGER,
    response_body TEXT,
    attempt INTEGER DEFAULT 1,
    max_attempts INTEGER DEFAULT 3,
    status VARCHAR(30) DEFAULT 'PENDING',
    next_retry_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_webhook_endpoints_org ON webhook_endpoints(organization_id);
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_endpoint ON webhook_deliveries(endpoint_id);
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_status ON webhook_deliveries(status);

-- =========================
-- 6. Candidate Feedback (Reverse)
-- =========================
CREATE TABLE IF NOT EXISTS candidate_feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id),
    interview_id UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    candidate_id UUID NOT NULL REFERENCES users(id),
    overall_rating INTEGER NOT NULL CHECK (overall_rating BETWEEN 1 AND 5),
    communication_rating INTEGER CHECK (communication_rating BETWEEN 1 AND 5),
    professionalism_rating INTEGER CHECK (professionalism_rating BETWEEN 1 AND 5),
    technical_clarity_rating INTEGER CHECK (technical_clarity_rating BETWEEN 1 AND 5),
    timeliness_rating INTEGER CHECK (timeliness_rating BETWEEN 1 AND 5),
    comments TEXT,
    would_recommend BOOLEAN,
    is_anonymous BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(interview_id, candidate_id)
);

CREATE INDEX IF NOT EXISTS idx_candidate_feedback_interview ON candidate_feedback(interview_id);
CREATE INDEX IF NOT EXISTS idx_candidate_feedback_candidate ON candidate_feedback(candidate_id);

-- =========================
-- 7. Activity Feed / Timeline
-- =========================
CREATE TABLE IF NOT EXISTS activity_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id),
    actor_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    target_type VARCHAR(50),
    target_id UUID,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_activity_events_actor ON activity_events(actor_id);
CREATE INDEX IF NOT EXISTS idx_activity_events_entity ON activity_events(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_activity_events_org ON activity_events(organization_id);
CREATE INDEX IF NOT EXISTS idx_activity_events_created ON activity_events(created_at DESC);

-- =========================
-- 8. Export/Import Jobs
-- =========================
CREATE TABLE IF NOT EXISTS export_import_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id),
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(20) NOT NULL,
    format VARCHAR(20) NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    entity_type VARCHAR(50) NOT NULL,
    filters JSONB,
    file_name VARCHAR(300),
    s3_key VARCHAR(500),
    total_records INTEGER,
    processed_records INTEGER DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_export_import_jobs_user ON export_import_jobs(user_id);
CREATE INDEX IF NOT EXISTS idx_export_import_jobs_status ON export_import_jobs(status);
