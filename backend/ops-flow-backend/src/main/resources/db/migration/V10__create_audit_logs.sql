CREATE TABLE audit_logs
(
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,

    user_id UUID,

    action VARCHAR(150) NOT NULL,

    resource_type VARCHAR(100) NOT NULL,

    resource_id UUID,

    old_data JSONB,

    new_data JSONB,

    ip_address VARCHAR(64),

    user_agent TEXT,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_audit_organization
        FOREIGN KEY (organization_id)
            REFERENCES organizations(id),

    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
);

CREATE INDEX idx_audit_organization
    ON audit_logs(organization_id);

CREATE INDEX idx_audit_user
    ON audit_logs(user_id);

CREATE INDEX idx_audit_resource
    ON audit_logs(resource_type, resource_id);

CREATE INDEX idx_audit_created_at
    ON audit_logs(created_at);