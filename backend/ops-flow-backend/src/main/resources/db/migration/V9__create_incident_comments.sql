CREATE TABLE incident_comments
(
    id UUID PRIMARY KEY,

    incident_id UUID NOT NULL,

    user_id UUID NOT NULL,

    comment TEXT NOT NULL,

    is_edited BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_comments_incident
        FOREIGN KEY (incident_id)
            REFERENCES incidents(id),

    CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
);

CREATE INDEX idx_comments_incident
    ON incident_comments(incident_id);