-- Interview Pipelines: defines a hiring process with ordered stages
CREATE TABLE IF NOT EXISTS interview_pipelines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    department VARCHAR(200),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Pipeline Stages: ordered steps within a pipeline
CREATE TABLE IF NOT EXISTS pipeline_stages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pipeline_id UUID NOT NULL REFERENCES interview_pipelines(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    order_index INTEGER NOT NULL,
    interview_type VARCHAR(30),
    template_id UUID REFERENCES interview_templates(id) ON DELETE SET NULL,
    duration_minutes INTEGER,
    is_optional BOOLEAN NOT NULL DEFAULT false
);

-- Candidate Pipelines: tracks a candidate through a pipeline
CREATE TABLE IF NOT EXISTS candidate_pipelines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pipeline_id UUID NOT NULL REFERENCES interview_pipelines(id) ON DELETE CASCADE,
    candidate_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    current_stage_id UUID REFERENCES pipeline_stages(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(pipeline_id, candidate_id)
);

-- Candidate Stage Progress: tracks progress at each stage
CREATE TABLE IF NOT EXISTS candidate_stage_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_pipeline_id UUID NOT NULL REFERENCES candidate_pipelines(id) ON DELETE CASCADE,
    stage_id UUID NOT NULL REFERENCES pipeline_stages(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    interview_id UUID REFERENCES interviews(id) ON DELETE SET NULL,
    feedback TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_pipelines_active ON interview_pipelines(is_active);
CREATE INDEX IF NOT EXISTS idx_pipelines_department ON interview_pipelines(department);
CREATE INDEX IF NOT EXISTS idx_pipeline_stages_pipeline ON pipeline_stages(pipeline_id);
CREATE INDEX IF NOT EXISTS idx_candidate_pipelines_pipeline ON candidate_pipelines(pipeline_id);
CREATE INDEX IF NOT EXISTS idx_candidate_pipelines_candidate ON candidate_pipelines(candidate_id);
CREATE INDEX IF NOT EXISTS idx_candidate_pipelines_status ON candidate_pipelines(status);
CREATE INDEX IF NOT EXISTS idx_stage_progress_cp ON candidate_stage_progress(candidate_pipeline_id);
CREATE INDEX IF NOT EXISTS idx_stage_progress_stage ON candidate_stage_progress(stage_id);

