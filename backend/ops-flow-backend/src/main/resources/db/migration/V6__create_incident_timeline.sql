CREATE TABLE incident_timeline
(
    id UUID PRIMARY KEY,

    incident_id UUID NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    actor_id UUID,

    old_value TEXT,

    new_value TEXT,

    metadata JSONB,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_timeline_incident
        FOREIGN KEY (incident_id)
            REFERENCES incidents(id),

    CONSTRAINT fk_timeline_actor
        FOREIGN KEY (actor_id)
            REFERENCES users(id)
);

CREATE INDEX idx_timeline_incident
    ON incident_timeline(incident_id);

CREATE INDEX idx_timeline_created_at
    ON incident_timeline(created_at);