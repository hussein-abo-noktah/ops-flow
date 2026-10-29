package com.husseinabonoktah.opsflowbackend.incident.application;

import com.husseinabonoktah.opsflowbackend.incident.domain.*;
import com.husseinabonoktah.opsflowbackend.incident.api.request.CreateIncidentRequest;
import com.husseinabonoktah.opsflowbackend.incident.api.response.*;
import com.husseinabonoktah.opsflowbackend.organization.domain.Organization;
import com.husseinabonoktah.opsflowbackend.user.domain.User;
import org.springframework.stereotype.Component;
import java.util.UUID;
@Component
public class IncidentMapper {
    public Incident toEntity(CreateIncidentRequest request, Organization organization, User actor) {
        return Incident.open(organization, "INC-" + UUID.randomUUID(), request.title(),
                request.description(), request.severity(), actor);
    }
    public IncidentResponse toResponse(Incident i) {
        return new IncidentResponse(i.getId(), i.getOrganization().getId(), i.getIncidentNumber(),
                i.getTitle(), i.getDescription(), i.getSeverity(), i.getStatus(),
                i.getAssignedTeam() == null ? null : i.getAssignedTeam().getId(),
                i.getAssignedUser() == null ? null : i.getAssignedUser().getId(), i.getCreatedBy().getId(),
                i.getAcknowledgedAt(), i.getMitigatedAt(), i.getResolvedAt(), i.getClosedAt(),
                i.getCreatedAt(), i.getUpdatedAt(), i.getVersion());
    }
    public IncidentTimelineResponse toResponse(IncidentTimeline e) {
        return new IncidentTimelineResponse(e.getId(), e.getIncident().getId(), e.getEventType(),
                e.getActor() == null ? null : e.getActor().getId(), e.getOldValue(), e.getNewValue(),
                e.getMetadata(), e.getCreatedAt(), e.getComment() == null ? null : e.getComment().getId());
    }
}
