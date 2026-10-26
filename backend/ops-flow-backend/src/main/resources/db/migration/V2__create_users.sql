CREATE TABLE users
(
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,

    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,

    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    status VARCHAR(30) NOT NULL,

    last_login_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_users_organization
        FOREIGN KEY (organization_id)
            REFERENCES organizations(id),

    CONSTRAINT uk_users_email
        UNIQUE (email)
);

CREATE INDEX idx_users_organization
    ON users(organization_id);

CREATE INDEX idx_users_status
    ON users(status);