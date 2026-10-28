INSERT INTO permissions (id, code, description)
VALUES
    (gen_random_uuid(), 'incident:create',  'Create incidents'),
    (gen_random_uuid(), 'incident:view',    'View incidents'),
    (gen_random_uuid(), 'incident:update',  'Update incidents'),
    (gen_random_uuid(), 'incident:assign',  'Assign incidents'),
    (gen_random_uuid(), 'incident:resolve', 'Resolve incidents'),

    (gen_random_uuid(), 'user:view',   'View users'),
    (gen_random_uuid(), 'user:manage', 'Manage users'),

    (gen_random_uuid(), 'team:create', 'Create teams'),
    (gen_random_uuid(), 'team:update', 'Update teams'),

    (gen_random_uuid(), 'audit:view', 'View audit logs');