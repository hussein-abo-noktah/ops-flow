-- Keep code uniqueness consistent with API normalization, including concurrent writes.
CREATE UNIQUE INDEX uk_teams_org_code_ignore_case ON teams (organization_id, lower(code));

-- A user may have several memberships, but at most one primary team.
CREATE UNIQUE INDEX uk_team_members_primary_user ON team_members (user_id) WHERE is_primary;
