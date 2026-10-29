package com.husseinabonoktah.opsflowbackend.incident.domain;

import com.husseinabonoktah.opsflowbackend.shared.persistence.BaseEntity;
import com.husseinabonoktah.opsflowbackend.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import java.time.Instant;

@Entity
@Getter
@Immutable
@Table(name = "incident_comment_revisions")
public class IncidentCommentRevision extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false, updatable = false)
    private IncidentComment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editor_id", updatable = false)
    private User editor;

    @Column(name = "revision_number", nullable = false, updatable = false)
    private Long revisionNumber;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "revision_type", nullable = false, updatable = false, length = 30)
    private CommentRevisionType revisionType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IncidentCommentRevision() {}

    public IncidentCommentRevision(IncidentComment comment, User editor, CommentRevisionType type) {
        this.comment = comment;
        this.editor = editor;
        this.revisionNumber = comment.getVersion();
        this.content = comment.getComment();
        this.revisionType = type;
        this.createdAt = Instant.now();
    }
}
