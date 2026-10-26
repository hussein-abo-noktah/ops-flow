CREATE TABLE team_members
(
    id UUID PRIMARY KEY,

    team_id UUID NOT NULL,
    user_id UUID NOT NULL,

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    joined_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_team_members_team
        FOREIGN KEY (team_id)
            REFERENCES teams(id),

    CONSTRAINT fk_team_members_user
        FOREIGN KEY (user_id)
            REFERENCES users(id),

    CONSTRAINT uk_team_members_team_user
        UNIQUE (team_id, user_id)
);

CREATE INDEX idx_team_members_team
    ON team_members(team_id);

CREATE INDEX idx_team_members_user
    ON team_members(user_id);