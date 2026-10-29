package com.husseinabonoktah.opsflowbackend.incident.application;

import com.husseinabonoktah.opsflowbackend.incident.domain.*;
import com.husseinabonoktah.opsflowbackend.incident.api.response.*;
import org.springframework.stereotype.Component;

@Component
public class IncidentCommentMapper {
    public IncidentCommentResponse toResponse(IncidentComment comment) {
        return new IncidentCommentResponse(comment.getId(), comment.getIncident().getId(), comment.getUser().getId(),
                comment.getComment(), comment.isEdited(), comment.getVersion(), comment.getCreatedAt(), comment.getUpdatedAt());
    }

    public IncidentCommentRevisionResponse toResponse(IncidentCommentRevision revision) {
        return new IncidentCommentRevisionResponse(revision.getId(), revision.getComment().getId(),
                revision.getEditor() == null ? null : revision.getEditor().getId(), revision.getRevisionNumber(),
                revision.getContent(), revision.getRevisionType(), revision.getCreatedAt());
    }
}
