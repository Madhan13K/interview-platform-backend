-- =====================================================
-- Feature 8: Automated Scheduling (Availability Slots)
-- =====================================================
CREATE TABLE IF NOT EXISTS availability_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL, -- 0=Monday, 6=Sunday
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    time_zone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    is_recurring BOOLEAN NOT NULL DEFAULT true,
    specific_date DATE,
    is_available BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_availability_user ON availability_slots(user_id);
CREATE INDEX IF NOT EXISTS idx_availability_day ON availability_slots(day_of_week);
CREATE INDEX IF NOT EXISTS idx_availability_date ON availability_slots(specific_date);

-- =====================================================
-- Feature 9: Interview Reminders
-- =====================================================
CREATE TABLE IF NOT EXISTS interview_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reminder_type VARCHAR(30) NOT NULL, -- BEFORE_24H, BEFORE_1H, BEFORE_15MIN
    channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL', -- EMAIL, SMS, PUSH
    scheduled_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, FAILED, CANCELLED
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reminders_interview ON interview_reminders(interview_id);
CREATE INDEX IF NOT EXISTS idx_reminders_user ON interview_reminders(user_id);
CREATE INDEX IF NOT EXISTS idx_reminders_status ON interview_reminders(status);
CREATE INDEX IF NOT EXISTS idx_reminders_scheduled ON interview_reminders(scheduled_at);

-- =====================================================
-- Feature 10: Candidate Self-Service (Preferred Slots)
-- =====================================================
CREATE TABLE IF NOT EXISTS candidate_preferred_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    interview_id UUID REFERENCES interviews(id) ON DELETE SET NULL,
    job_position_id UUID REFERENCES job_positions(id) ON DELETE SET NULL,
    preferred_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    time_zone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    priority INTEGER NOT NULL DEFAULT 1, -- 1=highest priority
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED', -- SUBMITTED, ACCEPTED, REJECTED
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_preferred_slots_candidate ON candidate_preferred_slots(candidate_id);
CREATE INDEX IF NOT EXISTS idx_preferred_slots_interview ON candidate_preferred_slots(interview_id);
CREATE INDEX IF NOT EXISTS idx_preferred_slots_position ON candidate_preferred_slots(job_position_id);

-- =====================================================
-- Feature 11: Team/Department Management
-- =====================================================
CREATE TABLE IF NOT EXISTS teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    department VARCHAR(200),
    manager_id UUID REFERENCES users(id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS team_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER', -- LEAD, MEMBER, OBSERVER
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(team_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_teams_department ON teams(department);
CREATE INDEX IF NOT EXISTS idx_teams_manager ON teams(manager_id);
CREATE INDEX IF NOT EXISTS idx_team_members_team ON team_members(team_id);
CREATE INDEX IF NOT EXISTS idx_team_members_user ON team_members(user_id);

-- =====================================================
-- Feature 12: Tags & Labels
-- =====================================================
CREATE TABLE IF NOT EXISTS tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    color VARCHAR(7) DEFAULT '#6c757d', -- hex color
    category VARCHAR(50), -- INTERVIEW, CANDIDATE, QUESTION, GENERAL
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS entity_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL, -- INTERVIEW, USER, QUESTION, JOB_POSITION
    entity_id UUID NOT NULL,
    tagged_by UUID REFERENCES users(id) ON DELETE SET NULL,
    tagged_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(tag_id, entity_type, entity_id)
);

CREATE INDEX IF NOT EXISTS idx_tags_name ON tags(name);
CREATE INDEX IF NOT EXISTS idx_tags_category ON tags(category);
CREATE INDEX IF NOT EXISTS idx_entity_tags_entity ON entity_tags(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_entity_tags_tag ON entity_tags(tag_id);

