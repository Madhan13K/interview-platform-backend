-- V27: Approval Workflows - Configurable approval chains
CREATE TABLE approval_chains (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,
    approval_mode   VARCHAR(20) NOT NULL DEFAULT 'SEQUENTIAL',
    tenant_id       UUID,
    created_by      UUID REFERENCES users(id),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_approval_chains_type ON approval_chains(entity_type, active);

CREATE TABLE approval_steps (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chain_id        UUID NOT NULL REFERENCES approval_chains(id) ON DELETE CASCADE,
    step_order      INTEGER NOT NULL,
    approver_role   VARCHAR(100),
    approver_id     UUID,
    required        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_approval_steps_chain ON approval_steps(chain_id, step_order);

CREATE TABLE approval_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chain_id        UUID NOT NULL REFERENCES approval_chains(id),
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_by    UUID NOT NULL REFERENCES users(id),
    requested_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_approval_requests_entity ON approval_requests(entity_type, entity_id);
CREATE INDEX idx_approval_requests_status ON approval_requests(status);

CREATE TABLE approval_decisions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id      UUID NOT NULL REFERENCES approval_requests(id) ON DELETE CASCADE,
    step_id         UUID NOT NULL REFERENCES approval_steps(id),
    approver_id     UUID NOT NULL REFERENCES users(id),
    decision        VARCHAR(20) NOT NULL,
    comments        TEXT,
    decided_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_approval_decisions_request ON approval_decisions(request_id);
