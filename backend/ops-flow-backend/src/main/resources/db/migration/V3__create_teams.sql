CREATE TABLE teams
(
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,

    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL,

    description TEXT,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_teams_organization
        FOREIGN KEY (organization_id)
            REFERENCES organizations(id),

    CONSTRAINT uk_teams_org_code
        UNIQUE (organization_id, code)
);

CREATE INDEX idx_teams_organization
    ON teams(organization_id);

CREATE INDEX idx_teams_status
    ON teams(status);