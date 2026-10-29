package com.husseinabonoktah.opsflowbackend.incident.application;

import com.husseinabonoktah.opsflowbackend.incident.api.request.*;
import com.husseinabonoktah.opsflowbackend.incident.api.response.IncidentResponse;
import com.husseinabonoktah.opsflowbackend.incident.domain.*;
import com.husseinabonoktah.opsflowbackend.incident.infrastructure.persistence.IncidentRepository;
import com.husseinabonoktah.opsflowbackend.organization.infrastructure.persistence.OrganizationRepository;
import com.husseinabonoktah.opsflowbackend.team.domain.Team;
import com.husseinabonoktah.opsflowbackend.team.domain.TeamStatus;
import com.husseinabonoktah.opsflowbackend.team.infrastructure.persistence.TeamRepository;
import com.husseinabonoktah.opsflowbackend.user.domain.*;
import com.husseinabonoktah.opsflowbackend.user.infrastructure.persistence.UserRepository;
import com.husseinabonoktah.opsflowbackend.shared.exception.*;
import com.husseinabonoktah.opsflowbackend.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.util.*;
import java.util.function.Consumer;
import tools.jackson.databind.ObjectMapper;
import static com.husseinabonoktah.opsflowbackend.incident.infrastructure.persistence.specification.IncidentSpecifications.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentService {
    private final IncidentRepository repository;
    private final OrganizationRepository organizations;
    private final UserRepository users;
    private final TeamRepository teams;
    private final CurrentUser currentUser;
    private final IncidentMapper mapper;
    private final IncidentTimelineService timeline;
    private final ObjectMapper json;

    @Transactional
    public IncidentResponse create(CreateIncidentRequest request) {
        var organization = organizations.findById(currentUser.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found."));
        User actor = actor();
        Incident incident = repository.saveAndFlush(mapper.toEntity(request, organization, actor));
        timeline.record(incident, TimelineEventType.INCIDENT_CREATED, actor, null, incident.getStatus().name());
        return mapper.toResponse(incident);
    }
    public IncidentResponse getById(UUID id) { return mapper.toResponse(findById(id)); }
    public Page<IncidentResponse> search(IncidentSearchRequest request) {
        var specification = inOrganization(currentUser.organizationId())
                .and(matchingText(request.q()))
                .and(hasStatus(request.status()))
                .and(hasSeverity(request.severity()))
                .and(assignedToTeam(request.assignedTeamId()))
                .and(assignedToUser(request.assignedUserId()))
                .and(createdBy(request.createdBy()))
                .and(createdFrom(request.createdFrom()))
                .and(createdTo(request.createdTo()));
        return repository.findAll(specification, request.toPageable()).map(mapper::toResponse);
    }
    @Transactional
    public IncidentResponse update(UUID id, UpdateIncidentRequest request) {
        Incident incident = findById(id);
        String before = details(incident);
        incident.updateDetails(request.title(), request.description());
        return save(incident, TimelineEventType.INCIDENT_UPDATED, before, details(incident));
    }
    @Transactional
    public IncidentResponse assign(UUID id, AssignIncidentRequest request) {
        Incident incident = findById(id);
        Team team = request.teamId() == null ? null : teams.findByIdAndOrganizationId(
                request.teamId(), currentUser.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + request.teamId()));
        User user = request.userId() == null ? null : tenantUser(request.userId());
        if ((team != null && team.getStatus() != TeamStatus.ACTIVE)
                || (user != null && user.getStatus() != UserStatus.ACTIVE)) {
            throw new ResourceConflictException("Assignees must be active.");
        }
        String before = assignment(incident);
        incident.assign(team, user);
        return save(incident, TimelineEventType.ASSIGNED, before, assignment(incident));
    }
    @Transactional
    public IncidentResponse changeSeverity(UUID id, ChangeSeverityRequest request) {
        Incident incident = findById(id);
        String before = incident.getSeverity().name();
        incident.changeSeverity(request.severity());
        return save(incident, TimelineEventType.SEVERITY_CHANGED, before, incident.getSeverity().name());
    }
    @Transactional
    public IncidentResponse acknowledge(UUID id) { return transition(id, Incident::acknowledge); }
    @Transactional
    public IncidentResponse startInvestigation(UUID id) { return transition(id, Incident::startInvestigation); }
    @Transactional
    public IncidentResponse mitigate(UUID id) { return transition(id, Incident::mitigate); }
    @Transactional
    public IncidentResponse resolve(UUID id) { return transition(id, Incident::resolve); }
    @Transactional
    public IncidentResponse close(UUID id) { return transition(id, Incident::close); }

    private IncidentResponse transition(UUID id, Consumer<Incident> action) {
        Incident incident = findById(id);
        String before = incident.getStatus().name();
        action.accept(incident);
        return save(incident, TimelineEventType.STATUS_CHANGED, before, incident.getStatus().name());
    }
    private IncidentResponse save(Incident incident, TimelineEventType type, String before, String after) {
        if (!Objects.equals(before, after)) {
            User actor = actor();
            repository.saveAndFlush(incident);
            timeline.record(incident, type, actor, before, after);
        }
        return mapper.toResponse(incident);
    }
    private Incident findById(UUID id) {
        return repository.findByIdAndOrganizationId(id, currentUser.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
    }
    private User actor() { return tenantUser(currentUser.id()); }
    private User tenantUser(UUID id) {
        return users.findById(id).filter(u -> u.getOrganization().getId().equals(currentUser.organizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
    private String details(Incident incident) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("title", incident.getTitle());
        values.put("description", incident.getDescription());
        return json.writeValueAsString(values);
    }
    private String assignment(Incident incident) {
        Map<String, UUID> values = new LinkedHashMap<>();
        values.put("teamId", incident.getAssignedTeam() == null ? null : incident.getAssignedTeam().getId());
        values.put("userId", incident.getAssignedUser() == null ? null : incident.getAssignedUser().getId());
        return json.writeValueAsString(values);
    }
}
