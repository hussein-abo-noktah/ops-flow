ALTER TABLE incident_comments ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE incident_timeline ADD COLUMN comment_id UUID REFERENCES incident_comments(id);

CREATE TABLE incident_comment_revisions (
    id UUID PRIMARY KEY,
    comment_id UUID NOT NULL REFERENCES incident_comments(id),
    editor_id UUID REFERENCES users(id),
    revision_number BIGINT NOT NULL CHECK (revision_number >= 0),
    content TEXT NOT NULL,
    revision_type VARCHAR(30) NOT NULL
        CHECK (revision_type IN ('CREATED', 'EDITED', 'LEGACY_SNAPSHOT')),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_comment_revision UNIQUE (comment_id, revision_number)
);

-- Existing rows have no trustworthy prior edit history or editor identity.
INSERT INTO incident_comment_revisions
    (id, comment_id, editor_id, revision_number, content, revision_type, created_at)
SELECT gen_random_uuid(), id, NULL, 0, comment, 'LEGACY_SNAPSHOT', CURRENT_TIMESTAMP
FROM incident_comments;

CREATE INDEX idx_comments_incident_created_id ON incident_comments (incident_id, created_at, id);
CREATE INDEX idx_timeline_incident_created_id ON incident_timeline (incident_id, created_at, id);
CREATE INDEX idx_timeline_comment ON incident_timeline (comment_id);

CREATE FUNCTION reject_incident_history_mutation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Incident history is append-only: % on % is prohibited', TG_OP, TG_TABLE_NAME
        USING ERRCODE = '23514';
END;
$$;

CREATE TRIGGER timeline_no_mutation BEFORE UPDATE OR DELETE ON incident_timeline
    FOR EACH ROW EXECUTE FUNCTION reject_incident_history_mutation();
CREATE TRIGGER timeline_no_truncate BEFORE TRUNCATE ON incident_timeline
    FOR EACH STATEMENT EXECUTE FUNCTION reject_incident_history_mutation();
CREATE TRIGGER comment_revisions_no_mutation BEFORE UPDATE OR DELETE ON incident_comment_revisions
    FOR EACH ROW EXECUTE FUNCTION reject_incident_history_mutation();
CREATE TRIGGER comment_revisions_no_truncate BEFORE TRUNCATE ON incident_comment_revisions
    FOR EACH STATEMENT EXECUTE FUNCTION reject_incident_history_mutation();

CREATE FUNCTION protect_incident_comment_identity() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.incident_id IS DISTINCT FROM OLD.incident_id
        OR NEW.user_id IS DISTINCT FROM OLD.user_id
        OR NEW.created_at IS DISTINCT FROM OLD.created_at
        OR NEW.version <> OLD.version + 1
        OR NEW.comment IS NOT DISTINCT FROM OLD.comment
        OR NOT NEW.is_edited THEN
        RAISE EXCEPTION 'Comment identity is immutable; edits require a new version and changed content'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER comment_protect_identity BEFORE UPDATE ON incident_comments
    FOR EACH ROW EXECUTE FUNCTION protect_incident_comment_identity();

CREATE FUNCTION validate_incident_comment_revision() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM incident_comments c
        WHERE c.id = NEW.comment_id AND c.version = NEW.revision_number
            AND c.comment = NEW.content AND c.user_id = NEW.editor_id
            AND ((NEW.revision_number = 0 AND NEW.revision_type = 'CREATED')
              OR (NEW.revision_number > 0 AND NEW.revision_type = 'EDITED'))
    ) THEN
        RAISE EXCEPTION 'Revision must describe the current comment and its author'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER comment_revision_validate BEFORE INSERT ON incident_comment_revisions
    FOR EACH ROW EXECUTE FUNCTION validate_incident_comment_revision();

-- Deferred checks permit the application to write comment, revision, and timeline
-- in one transaction, while rejecting direct SQL edits that omit their history.
CREATE FUNCTION require_incident_comment_history() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    expected_event VARCHAR(100);
    previous_content TEXT;
BEGIN
    IF TG_OP = 'INSERT' THEN
        expected_event := 'COMMENT_ADDED';
        previous_content := NULL;
    ELSE
        expected_event := 'COMMENT_EDITED';
        previous_content := OLD.comment;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM incident_comment_revisions r
        WHERE r.comment_id = NEW.id AND r.revision_number = NEW.version AND r.content = NEW.comment
    ) OR NOT EXISTS (
        SELECT 1 FROM incident_timeline e
        WHERE e.comment_id = NEW.id AND e.incident_id = NEW.incident_id
            AND e.actor_id = NEW.user_id AND e.event_type = expected_event
            AND e.new_value = NEW.comment AND e.old_value IS NOT DISTINCT FROM previous_content
            AND e.metadata ->> 'revision' = NEW.version::TEXT
    ) THEN
        RAISE EXCEPTION 'Every comment change requires a matching immutable revision and timeline event'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE CONSTRAINT TRIGGER comment_require_history AFTER INSERT OR UPDATE ON incident_comments
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION require_incident_comment_history();
