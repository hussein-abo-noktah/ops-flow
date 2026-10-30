-- Optional local-development seed. Apply manually AFTER Flyway migrations.
-- Email: admin@opsflow.local
-- Password: OpsFlow@Admin123!
-- This directory is deliberately outside Flyway's db/migration location.
-- Never apply these publicly known credentials to a shared or production database.

BEGIN;

-- Serialize with other seed runs and the application's BootstrapAdmin runner.
LOCK TABLE users IN EXCLUSIVE MODE;

DO $$
DECLARE
    admin_email CONSTANT TEXT := 'admin@opsflow.local';
    organization_id UUID := gen_random_uuid();
    admin_id UUID := gen_random_uuid();
    admin_role_id UUID := gen_random_uuid();
BEGIN
    -- Preserve existing credentials, tenant, status, and privileges, including
    -- accounts stored before email normalization was introduced.
    IF EXISTS (SELECT 1 FROM users WHERE lower(email) = admin_email) THEN
        RAISE NOTICE 'Admin seed skipped: an account with email % already exists.', admin_email;
        RETURN;
    END IF;

    INSERT INTO organizations (id, name, slug, status, created_at, updated_at)
    VALUES (organization_id, 'Ops Flow Development', 'dev-admin-' || organization_id,
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    INSERT INTO roles (id, organization_id, name, code, description, is_system, created_at, updated_at)
    VALUES (admin_role_id, organization_id, 'Platform Admin', 'PLATFORM_ADMIN',
            'Local development administrator', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    INSERT INTO users (id, organization_id, first_name, last_name, email, password_hash,
                       status, created_at, updated_at)
    VALUES (admin_id, organization_id, 'Platform', 'Admin', admin_email,
            '$2a$10$jPsGwYoWHLUJcd2kBmMcbua9zn.Q3kqGJPw9TyCTRi1GTzNmD0c52',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    INSERT INTO user_roles (user_id, role_id) VALUES (admin_id, admin_role_id);
END;
$$;

COMMIT;
