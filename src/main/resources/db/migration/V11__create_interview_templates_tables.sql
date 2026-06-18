-- Interview Templates table
CREATE TABLE IF NOT EXISTS interview_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    type VARCHAR(30) NOT NULL,
    mode VARCHAR(20) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    evaluation_criteria TEXT,
    instructions TEXT,
    tags VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Template Questions junction table
CREATE TABLE IF NOT EXISTS template_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES interview_templates(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    order_index INTEGER NOT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT true,
    time_allocation_minutes INTEGER,
    notes VARCHAR(500),
    UNIQUE(template_id, question_id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_interview_templates_type ON interview_templates(type);
CREATE INDEX IF NOT EXISTS idx_interview_templates_active ON interview_templates(is_active);
CREATE INDEX IF NOT EXISTS idx_interview_templates_created_by ON interview_templates(created_by);
CREATE INDEX IF NOT EXISTS idx_template_questions_template ON template_questions(template_id);
CREATE INDEX IF NOT EXISTS idx_template_questions_question ON template_questions(question_id);
