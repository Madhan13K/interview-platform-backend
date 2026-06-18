-- ─────────────────────────────────────────────────────────────
-- INTERVIEWER AVAILABILITY (Calendar Integration)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS interviewer_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interviewer_id UUID NOT NULL,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    time_zone VARCHAR(100) NOT NULL DEFAULT 'UTC',
    is_recurring BOOLEAN NOT NULL DEFAULT TRUE,
    specific_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_availability_interviewer
        FOREIGN KEY (interviewer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_time_range CHECK (end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_availability_interviewer ON interviewer_availability(interviewer_id);
CREATE INDEX IF NOT EXISTS idx_availability_day ON interviewer_availability(day_of_week);
CREATE INDEX IF NOT EXISTS idx_availability_specific_date ON interviewer_availability(specific_date);

-- ─────────────────────────────────────────────────────────────
-- QUESTION BANK
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS question_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    category_id UUID NOT NULL,
    difficulty VARCHAR(20) NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    type VARCHAR(50) NOT NULL CHECK (type IN ('CODING', 'SYSTEM_DESIGN', 'BEHAVIORAL', 'THEORETICAL', 'MCQ')),
    expected_duration_minutes INTEGER,
    sample_answer TEXT,
    hints TEXT,
    tags VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_question_category
        FOREIGN KEY (category_id) REFERENCES question_categories(id),
    CONSTRAINT fk_question_creator
        FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_questions_category ON questions(category_id);
CREATE INDEX IF NOT EXISTS idx_questions_difficulty ON questions(difficulty);
CREATE INDEX IF NOT EXISTS idx_questions_type ON questions(type);
CREATE INDEX IF NOT EXISTS idx_questions_active ON questions(is_active);

-- ─────────────────────────────────────────────────────────────
-- INTERVIEW ↔ QUESTION MAPPING
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS interview_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id UUID NOT NULL,
    question_id UUID NOT NULL,
    order_index INTEGER NOT NULL DEFAULT 0,
    notes TEXT,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_iq_interview
        FOREIGN KEY (interview_id) REFERENCES interviews(id) ON DELETE CASCADE,
    CONSTRAINT fk_iq_question
        FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT uk_interview_question UNIQUE (interview_id, question_id)
);

-- ─────────────────────────────────────────────────────────────
-- CODING SESSION (Collaborative Code Editor State)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS coding_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id UUID NOT NULL,
    language VARCHAR(50) NOT NULL DEFAULT 'java',
    code_content TEXT,
    last_edited_by UUID,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMP,
    CONSTRAINT fk_coding_session_interview
        FOREIGN KEY (interview_id) REFERENCES interviews(id) ON DELETE CASCADE,
    CONSTRAINT fk_coding_session_editor
        FOREIGN KEY (last_edited_by) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_coding_sessions_interview ON coding_sessions(interview_id);

-- ─────────────────────────────────────────────────────────────
-- MEETING LINKS (Generated meeting details)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS meeting_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id UUID NOT NULL UNIQUE,
    provider VARCHAR(50) NOT NULL CHECK (provider IN ('ZOOM', 'GOOGLE_MEET', 'TEAMS', 'INTERNAL')),
    meeting_url VARCHAR(1000) NOT NULL,
    host_url VARCHAR(1000),
    meeting_id VARCHAR(255),
    passcode VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    CONSTRAINT fk_meeting_link_interview
        FOREIGN KEY (interview_id) REFERENCES interviews(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────────────────────
-- SEED QUESTION CATEGORIES
-- ─────────────────────────────────────────────────────────────
INSERT INTO question_categories (name, description) VALUES
    ('Data Structures & Algorithms', 'Questions on arrays, trees, graphs, dynamic programming, etc.'),
    ('System Design', 'Architecture and system design questions'),
    ('Object-Oriented Design', 'OOP concepts, design patterns, SOLID principles'),
    ('Behavioral', 'Behavioral and situational interview questions'),
    ('Database', 'SQL, NoSQL, database design and optimization'),
    ('Concurrency', 'Multithreading, synchronization, concurrent programming'),
    ('Web Development', 'HTTP, REST, frontend/backend integration'),
    ('DevOps & Cloud', 'CI/CD, containerization, cloud services')
ON CONFLICT (name) DO NOTHING;

