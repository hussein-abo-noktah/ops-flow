package com.husseinabonoktah.opsflowbackend.incident.application;

import com.husseinabonoktah.opsflowbackend.incident.domain.*;
import com.husseinabonoktah.opsflowbackend.incident.api.response.IncidentTimelineResponse;
import com.husseinabonoktah.opsflowbackend.incident.api.request.IncidentTimelineQuery;
import com.husseinabonoktah.opsflowbackend.incident.infrastructure.persistence.*;
import com.husseinabonoktah.opsflowbackend.shared.exception.ResourceNotFoundException;
import com.husseinabonoktah.opsflowbackend.shared.security.CurrentUser;
import com.husseinabonoktah.opsflowbackend.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.data.domain.*;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentTimelineService {
    private final IncidentTimelineRepository repository;
    private final IncidentRepository incidents;
    private final CurrentUser currentUser;
    private final IncidentMapper mapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Incident incident, TimelineEventType type, User actor, String oldValue, String newValue) {
        repository.save(new IncidentTimeline(incident, type, actor, oldValue, newValue));
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordComment(IncidentComment comment, TimelineEventType type, User actor, String oldValue) {
        repository.save(new IncidentTimeline(comment.getIncident(), type, actor, oldValue, comment.getComment(),
                comment, "{\"revision\":" + comment.getVersion() + "}"));
    }

    public Page<IncidentTimelineResponse> search(UUID id, IncidentTimelineQuery request) {
        UUID organizationId = currentUser.organizationId();
        incidents.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
        return repository.search(id, organizationId, request.eventType(), request.actorId(), request.commentId(),
                request.from(), request.to(), request.toPageable()).map(mapper::toResponse);
    }
}
