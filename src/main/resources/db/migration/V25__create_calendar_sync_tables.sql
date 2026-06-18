-- V25: Calendar Sync - Bidirectional Google Calendar / Outlook sync
CREATE TABLE calendar_connections (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider        VARCHAR(30) NOT NULL,
    access_token    TEXT,
    refresh_token   TEXT,
    token_expires_at TIMESTAMP WITH TIME ZONE,
    calendar_id     VARCHAR(200) DEFAULT 'primary',
    sync_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    last_sync_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_calendar_connection_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX idx_calendar_connections_user ON calendar_connections(user_id);
CREATE INDEX idx_calendar_connections_sync ON calendar_connections(sync_enabled) WHERE sync_enabled = TRUE;

CREATE TABLE calendar_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    connection_id       UUID NOT NULL REFERENCES calendar_connections(id) ON DELETE CASCADE,
    interview_id        UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    external_event_id   VARCHAR(300),
    external_calendar_id VARCHAR(200),
    last_synced_at      TIMESTAMP WITH TIME ZONE,
    sync_direction      VARCHAR(20) NOT NULL DEFAULT 'OUTBOUND',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_calendar_event_connection_interview UNIQUE (connection_id, interview_id)
);

CREATE INDEX idx_calendar_events_connection ON calendar_events(connection_id);
CREATE INDEX idx_calendar_events_interview ON calendar_events(interview_id);
CREATE INDEX idx_calendar_events_external ON calendar_events(external_event_id);
