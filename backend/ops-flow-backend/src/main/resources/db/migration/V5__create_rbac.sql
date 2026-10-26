CREATE TABLE roles
(
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,

    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL,

    description TEXT,

    is_system BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_roles_organization
        FOREIGN KEY (organization_id)
            REFERENCES organizations(id),

    CONSTRAINT uk_roles_org_code
        UNIQUE (organization_id, code)
);


CREATE TABLE permissions
(
    id UUID PRIMARY KEY,

    code VARCHAR(150) NOT NULL,

    description TEXT,

    CONSTRAINT uk_permissions_code
        UNIQUE (code)
);

CREATE INDEX idx_roles_organization
    ON roles(organization_id);