CREATE TABLE IF NOT EXISTS interviews (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    candidate_id UUID NOT NULL,
    scheduled_by UUID NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    time_zone VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    mode VARCHAR(50) NOT NULL,
    meeting_link VARCHAR(1000),
    location VARCHAR(1000),
    cancel_reason VARCHAR(1000),
    reschedule_reason VARCHAR(1000),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_interview_candidate
        FOREIGN KEY (candidate_id) REFERENCES users(id),
    CONSTRAINT fk_interview_scheduled_by
        FOREIGN KEY (scheduled_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS interview_interviewers (
    id UUID PRIMARY KEY,
    interview_id UUID NOT NULL,
    interviewer_id UUID NOT NULL,
    primary_interviewer BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_at TIMESTAMP,
    CONSTRAINT fk_interview_interviewer_interview
        FOREIGN KEY (interview_id) REFERENCES interviews(id) ON DELETE CASCADE,
    CONSTRAINT fk_interview_interviewer_user
        FOREIGN KEY (interviewer_id) REFERENCES users(id),
    CONSTRAINT uk_interview_interviewer UNIQUE (interview_id, interviewer_id)
);

CREATE INDEX IF NOT EXISTS idx_interviews_candidate_id ON interviews(candidate_id);
CREATE INDEX IF NOT EXISTS idx_interviews_scheduled_by ON interviews(scheduled_by);
CREATE INDEX IF NOT EXISTS idx_interviews_start_time ON interviews(start_time);
CREATE INDEX IF NOT EXISTS idx_interview_interviewers_interview_id ON interview_interviewers(interview_id);
CREATE INDEX IF NOT EXISTS idx_interview_interviewers_interviewer_id ON interview_interviewers(interviewer_id);

