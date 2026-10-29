package com.husseinabonoktah.opsflowbackend.incident.application;

import com.husseinabonoktah.opsflowbackend.incident.api.request.*;
import com.husseinabonoktah.opsflowbackend.incident.api.response.*;
import com.husseinabonoktah.opsflowbackend.incident.domain.*;
import com.husseinabonoktah.opsflowbackend.incident.infrastructure.persistence.*;
import com.husseinabonoktah.opsflowbackend.shared.exception.*;
import com.husseinabonoktah.opsflowbackend.shared.security.CurrentUser;
import com.husseinabonoktah.opsflowbackend.user.domain.*;
import com.husseinabonoktah.opsflowbackend.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentCommentService {
    private final IncidentRepository incidents;
    private final IncidentCommentRepository comments;
    private final IncidentCommentRevisionRepository revisions;
    private final UserRepository users;
    private final CurrentUser currentUser;
    private final IncidentTimelineService timeline;
    private final IncidentCommentMapper mapper;

    @Transactional
    public IncidentCommentResponse create(UUID incidentId, CreateIncidentCommentRequest request) {
        Incident incident = findIncident(incidentId);
        User actor = actor();
        IncidentComment comment = comments.saveAndFlush(new IncidentComment(incident, actor, request.comment()));
        revisions.save(new IncidentCommentRevision(comment, actor, CommentRevisionType.CREATED));
        timeline.recordComment(comment, TimelineEventType.COMMENT_ADDED, actor, null);
        return mapper.toResponse(comment);
    }

    @Transactional
    public IncidentCommentResponse edit(UUID incidentId, UUID commentId, EditIncidentCommentRequest request) {
        IncidentComment comment = findComment(incidentId, commentId);
        User actor = actor();
        if (!comment.getUser().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Only the author can edit this comment.");
        }
        if (!comment.getVersion().equals(request.version())) {
            throw new ResourceConflictException("The comment was changed. Reload it and retry.");
        }
        if (comment.getComment().equals(request.comment())) {
            return mapper.toResponse(comment);
        }
        String before = comment.getComment();
        comment.edit(request.comment());
        comments.saveAndFlush(comment);
        revisions.save(new IncidentCommentRevision(comment, actor, CommentRevisionType.EDITED));
        timeline.recordComment(comment, TimelineEventType.COMMENT_EDITED, actor, before);
        return mapper.toResponse(comment);
    }

    public IncidentCommentResponse getById(UUID incidentId, UUID commentId) {
        return mapper.toResponse(findComment(incidentId, commentId));
    }

    public Page<IncidentCommentResponse> getAll(UUID incidentId, HistoryPageRequest request) {
        findIncident(incidentId);
        return comments.findAllByIncidentIdAndIncidentOrganizationId(incidentId, currentUser.organizationId(),
                request.toPageable("createdAt")).map(mapper::toResponse);
    }

    public Page<IncidentCommentRevisionResponse> history(UUID incidentId, UUID commentId, HistoryPageRequest request) {
        findComment(incidentId, commentId);
        return revisions.findAllByCommentIdAndCommentIncidentOrganizationId(commentId, currentUser.organizationId(),
                request.toPageable("revisionNumber")).map(mapper::toResponse);
    }

    private Incident findIncident(UUID id) {
        return incidents.findByIdAndOrganizationId(id, currentUser.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
    }

    private IncidentComment findComment(UUID incidentId, UUID commentId) {
        return comments.findByIdAndIncidentIdAndIncidentOrganizationId(commentId, incidentId, currentUser.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));
    }

    private User actor() {
        User actor = users.findById(currentUser.id())
                .filter(user -> user.getOrganization().getId().equals(currentUser.organizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (actor.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("An active account is required to comment.");
        }
        return actor;
    }
}
