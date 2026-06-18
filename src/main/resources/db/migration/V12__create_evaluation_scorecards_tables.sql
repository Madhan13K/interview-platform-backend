-- Evaluation Criteria: defines what to evaluate (e.g., Problem Solving, Communication)
CREATE TABLE IF NOT EXISTS evaluation_criteria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    interview_type VARCHAR(30),
    max_score INTEGER NOT NULL DEFAULT 5,
    weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    order_index INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Evaluation Scorecards: one per interview + interviewer combination
CREATE TABLE IF NOT EXISTS evaluation_scorecards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    interviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    overall_score DOUBLE PRECISION,
    recommendation VARCHAR(20),
    overall_comments TEXT,
    strengths TEXT,
    weaknesses TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    UNIQUE(interview_id, interviewer_id)
);

-- Scorecard Entries: individual scores per criteria
CREATE TABLE IF NOT EXISTS scorecard_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scorecard_id UUID NOT NULL REFERENCES evaluation_scorecards(id) ON DELETE CASCADE,
    criteria_id UUID NOT NULL REFERENCES evaluation_criteria(id) ON DELETE CASCADE,
    score INTEGER NOT NULL,
    comments VARCHAR(500)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_eval_criteria_type ON evaluation_criteria(interview_type);
CREATE INDEX IF NOT EXISTS idx_eval_criteria_active ON evaluation_criteria(is_active);
CREATE INDEX IF NOT EXISTS idx_eval_scorecards_interview ON evaluation_scorecards(interview_id);
CREATE INDEX IF NOT EXISTS idx_eval_scorecards_interviewer ON evaluation_scorecards(interviewer_id);
CREATE INDEX IF NOT EXISTS idx_scorecard_entries_scorecard ON scorecard_entries(scorecard_id);
CREATE INDEX IF NOT EXISTS idx_scorecard_entries_criteria ON scorecard_entries(criteria_id);

-- Seed default evaluation criteria (only if table is empty)
INSERT INTO evaluation_criteria (id, name, description, max_score, weight, order_index, is_active, created_at)
SELECT gen_random_uuid(), 'Problem Solving', 'Ability to analyze and solve problems logically', 5, 1.5, 1, true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM evaluation_criteria WHERE name = 'Problem Solving');

INSERT INTO evaluation_criteria (id, name, description, max_score, weight, order_index, is_active, created_at)
SELECT gen_random_uuid(), 'Communication', 'Clarity of thought, articulation, listening skills', 5, 1.0, 2, true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM evaluation_criteria WHERE name = 'Communication');

INSERT INTO evaluation_criteria (id, name, description, max_score, weight, order_index, is_active, created_at)
SELECT gen_random_uuid(), 'Technical Knowledge', 'Depth and breadth of technical understanding', 5, 1.5, 3, true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM evaluation_criteria WHERE name = 'Technical Knowledge');

INSERT INTO evaluation_criteria (id, name, description, max_score, weight, order_index, is_active, created_at)
SELECT gen_random_uuid(), 'Code Quality', 'Clean code, naming conventions, structure, best practices', 5, 1.2, 4, true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM evaluation_criteria WHERE name = 'Code Quality');

INSERT INTO evaluation_criteria (id, name, description, max_score, weight, order_index, is_active, created_at)
SELECT gen_random_uuid(), 'System Design', 'Ability to design scalable and maintainable systems', 5, 1.3, 5, true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM evaluation_criteria WHERE name = 'System Design');

INSERT INTO evaluation_criteria (id, name, description, max_score, weight, order_index, is_active, created_at)
SELECT gen_random_uuid(), 'Cultural Fit', 'Alignment with team values and collaboration style', 5, 0.8, 6, true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM evaluation_criteria WHERE name = 'Cultural Fit');

INSERT INTO evaluation_criteria (id, name, description, max_score, weight, order_index, is_active, created_at)
SELECT gen_random_uuid(), 'Leadership', 'Initiative, ownership, mentoring ability', 5, 0.7, 7, true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM evaluation_criteria WHERE name = 'Leadership');




