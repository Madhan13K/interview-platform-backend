-- Job Positions/Openings: represents open roles that interviews are linked to
CREATE TABLE IF NOT EXISTS job_positions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(300) NOT NULL,
    department VARCHAR(200),
    location VARCHAR(300),
    employment_type VARCHAR(50) NOT NULL DEFAULT 'FULL_TIME',
    experience_level VARCHAR(50) NOT NULL DEFAULT 'MID',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    description TEXT,
    requirements TEXT,
    responsibilities TEXT,
    salary_min DECIMAL(12,2),
    salary_max DECIMAL(12,2),
    salary_currency VARCHAR(10) DEFAULT 'USD',
    number_of_openings INTEGER NOT NULL DEFAULT 1,
    number_hired INTEGER NOT NULL DEFAULT 0,
    pipeline_id UUID REFERENCES interview_pipelines(id) ON DELETE SET NULL,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    hiring_manager_id UUID REFERENCES users(id) ON DELETE SET NULL,
    skills TEXT,
    posted_at TIMESTAMP,
    closed_at TIMESTAMP,
    deadline TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Link interviews to job positions
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS job_position_id UUID REFERENCES job_positions(id) ON DELETE SET NULL;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_job_positions_status ON job_positions(status);
CREATE INDEX IF NOT EXISTS idx_job_positions_department ON job_positions(department);
CREATE INDEX IF NOT EXISTS idx_job_positions_created_by ON job_positions(created_by);
CREATE INDEX IF NOT EXISTS idx_job_positions_hiring_manager ON job_positions(hiring_manager_id);
CREATE INDEX IF NOT EXISTS idx_interviews_job_position ON interviews(job_position_id);

