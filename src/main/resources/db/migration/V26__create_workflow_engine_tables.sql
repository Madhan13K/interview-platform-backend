-- V26: Configurable Workflow Engine - Rule-based automation
CREATE TABLE workflow_rules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(200) NOT NULL,
    description         TEXT,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    trigger_event       VARCHAR(50) NOT NULL,
    condition_type      VARCHAR(50) NOT NULL,
    condition_value     VARCHAR(500),
    action_type         VARCHAR(50) NOT NULL,
    action_config       TEXT,
    priority            INTEGER NOT NULL DEFAULT 100,
    tenant_id           UUID,
    created_by          UUID REFERENCES users(id),
    execution_count     BIGINT NOT NULL DEFAULT 0,
    last_executed_at    TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_workflow_rules_trigger ON workflow_rules(trigger_event, enabled);
CREATE INDEX idx_workflow_rules_tenant ON workflow_rules(tenant_id);
CREATE INDEX idx_workflow_rules_priority ON workflow_rules(priority);

CREATE TABLE workflow_executions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_rule_id    UUID NOT NULL REFERENCES workflow_rules(id) ON DELETE CASCADE,
    trigger_entity_type VARCHAR(100),
    trigger_entity_id   UUID,
    status              VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    execution_result    TEXT,
    executed_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    duration_ms         BIGINT
);

CREATE INDEX idx_workflow_executions_rule ON workflow_executions(workflow_rule_id);
CREATE INDEX idx_workflow_executions_entity ON workflow_executions(trigger_entity_type, trigger_entity_id);
CREATE INDEX idx_workflow_executions_time ON workflow_executions(executed_at DESC);
