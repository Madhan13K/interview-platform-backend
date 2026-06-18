-- V19: Code Execution Engine - sandboxed Docker-based code execution
CREATE TABLE code_executions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coding_session_id UUID NOT NULL REFERENCES coding_sessions(id) ON DELETE CASCADE,
    executed_by     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    language        VARCHAR(50) NOT NULL,
    source_code     TEXT NOT NULL,
    stdin           TEXT,
    stdout          TEXT,
    stderr          TEXT,
    exit_code       INTEGER,
    status          VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    execution_time_ms BIGINT,
    memory_used_bytes BIGINT,
    timeout_ms      BIGINT NOT NULL DEFAULT 10000,
    container_id    VARCHAR(80),
    error_message   TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    started_at      TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_code_executions_session ON code_executions(coding_session_id);
CREATE INDEX idx_code_executions_user ON code_executions(executed_by);
CREATE INDEX idx_code_executions_status ON code_executions(status);
CREATE INDEX idx_code_executions_created ON code_executions(created_at DESC);
