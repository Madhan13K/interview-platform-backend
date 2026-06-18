-- V21: Account Lockout - failed login tracking, IP blocking, security alerts
CREATE TABLE login_attempts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    ip_address      VARCHAR(45) NOT NULL,
    user_agent      VARCHAR(500),
    successful      BOOLEAN NOT NULL DEFAULT FALSE,
    failure_reason  VARCHAR(100),
    location        VARCHAR(200),
    attempted_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_login_attempts_email ON login_attempts(email);
CREATE INDEX idx_login_attempts_ip ON login_attempts(ip_address);
CREATE INDEX idx_login_attempts_time ON login_attempts(attempted_at DESC);
CREATE INDEX idx_login_attempts_email_failed ON login_attempts(email, successful, attempted_at)
    WHERE successful = FALSE;
CREATE INDEX idx_login_attempts_ip_failed ON login_attempts(ip_address, successful, attempted_at)
    WHERE successful = FALSE;

CREATE TABLE account_lockouts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked          BOOLEAN NOT NULL DEFAULT FALSE,
    locked_at       TIMESTAMP WITH TIME ZONE,
    lock_expires_at TIMESTAMP WITH TIME ZONE,
    last_failed_at  TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_account_lockouts_email ON account_lockouts(email);
CREATE INDEX idx_account_lockouts_locked ON account_lockouts(locked) WHERE locked = TRUE;

CREATE TABLE ip_blocklist (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ip_address      VARCHAR(45) NOT NULL UNIQUE,
    reason          VARCHAR(100) NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE,
    failed_attempts INTEGER,
    blocked_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    blocked_by      VARCHAR(200) DEFAULT 'SYSTEM',
    active          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_ip_blocklist_ip_active ON ip_blocklist(ip_address, active) WHERE active = TRUE;
CREATE INDEX idx_ip_blocklist_expires ON ip_blocklist(expires_at) WHERE active = TRUE AND expires_at IS NOT NULL;
