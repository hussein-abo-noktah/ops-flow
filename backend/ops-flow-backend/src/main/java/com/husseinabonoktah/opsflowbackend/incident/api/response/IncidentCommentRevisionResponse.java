package com.husseinabonoktah.opsflowbackend.incident.api.response;

import com.husseinabonoktah.opsflowbackend.incident.domain.CommentRevisionType;
import java.time.Instant;
import java.util.UUID;

public record IncidentCommentRevisionResponse(UUID id, UUID commentId, UUID editorId,
        Long revisionNumber, String content, CommentRevisionType revisionType, Instant createdAt) {}
