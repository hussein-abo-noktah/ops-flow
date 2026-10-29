CREATE INDEX idx_incidents_org_created_at_id
    ON incidents (organization_id, created_at, id);
