CREATE TABLE organizations
(
    id UUID PRIMARY KEY,

    name VARCHAR(150) NOT NULL,

    slug VARCHAR(100) NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_organizations_slug
        UNIQUE (slug)
);