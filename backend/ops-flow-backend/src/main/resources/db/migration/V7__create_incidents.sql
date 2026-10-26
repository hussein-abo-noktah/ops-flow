CREATE TABLE incidents
(
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,

    incident_number VARCHAR(50) NOT NULL,

    title VARCHAR(250) NOT NULL,

    description TEXT,

    severity VARCHAR(20) NOT NULL,

    status VARCHAR(30) NOT NULL,

    assigned_team_id UUID,

    assigned_user_id UUID,

    created_by UUID NOT NULL,

    acknowledged_at TIMESTAMPTZ,
    mitigated_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_incidents_organization
        FOREIGN KEY (organization_id)
            REFERENCES organizations(id),

    CONSTRAINT fk_incidents_assigned_team
        FOREIGN KEY (assigned_team_id)
            REFERENCES teams(id),

    CONSTRAINT fk_incidents_assigned_user
        FOREIGN KEY (assigned_user_id)
            REFERENCES users(id),

    CONSTRAINT fk_incidents_created_by
        FOREIGN KEY (created_by)
            REFERENCES users(id),

    CONSTRAINT uk_incidents_org_number
        UNIQUE (organization_id, incident_number)
);

CREATE INDEX idx_incidents_organization
    ON incidents(organization_id);

CREATE INDEX idx_incidents_status
    ON incidents(status);

CREATE INDEX idx_incidents_severity
    ON incidents(severity);

CREATE INDEX idx_incidents_assigned_team
    ON incidents(assigned_team_id);

CREATE INDEX idx_incidents_assigned_user
    ON incidents(assigned_user_id);

CREATE INDEX idx_incidents_created_at
    ON incidents(created_at);

CREATE INDEX idx_incidents_org_status
    ON incidents(organization_id, status);

CREATE INDEX idx_incidents_org_severity
    ON incidents(organization_id, severity);