package com.husseinabonoktah.opsflowbackend.incident.domain;



import com.husseinabonoktah.opsflowbackend.organization.domain.Organization;
import com.husseinabonoktah.opsflowbackend.team.domain.Team;
import com.husseinabonoktah.opsflowbackend.user.domain.User;
import com.husseinabonoktah.opsflowbackend.shared.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import com.husseinabonoktah.opsflowbackend.incident.domain.exception.InvalidIncidentTransitionException;
import com.husseinabonoktah.opsflowbackend.shared.exception.ResourceConflictException;

@Entity
@Table(
        name = "incidents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_incidents_org_number",
                        columnNames = {"organization_id", "incident_number"}
                )
        }
)
@Getter
public class Incident extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "incident_number", nullable = false, length = 50)
    private String incidentNumber;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_team_id")
    private Team assignedTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "mitigated_at")
    private Instant mitigatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(nullable = false)
    private Long version;


    protected Incident() {
    }

    public static Incident open(Organization organization, String incidentNumber, String title,
                                String description, Severity severity, User createdBy) {
        Incident incident = new Incident();
        incident.organization = Objects.requireNonNull(organization);
        incident.incidentNumber = Objects.requireNonNull(incidentNumber);
        incident.title = Objects.requireNonNull(title);
        incident.description = description;
        incident.severity = Objects.requireNonNull(severity);
        incident.createdBy = Objects.requireNonNull(createdBy);
        incident.status = IncidentStatus.OPEN;
        return incident;
    }

    public void acknowledge() {
        transition(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED);
        acknowledgedAt = Instant.now();
    }

    public void startInvestigation() {
        transition(IncidentStatus.ACKNOWLEDGED, IncidentStatus.INVESTIGATING);
    }

    public void mitigate() {
        transition(IncidentStatus.INVESTIGATING, IncidentStatus.MITIGATED);
        mitigatedAt = Instant.now();
    }

    public void resolve() {
        transition(IncidentStatus.MITIGATED, IncidentStatus.RESOLVED);
        resolvedAt = Instant.now();
    }

    public void close() {
        transition(IncidentStatus.RESOLVED, IncidentStatus.CLOSED);
        closedAt = Instant.now();
    }

    public void updateDetails(String title, String description) {
        requireEditable();
        this.title = Objects.requireNonNull(title);
        this.description = description;
    }

    public void assign(Team team, User user) {
        requireEditable();
        if ((team != null && !Objects.equals(organization.getId(), team.getOrganization().getId()))
                || (user != null && !Objects.equals(organization.getId(), user.getOrganization().getId()))) {
            throw new ResourceConflictException("Assignees must belong to the incident organization.");
        }
        assignedTeam = team;
        assignedUser = user;
    }

    public void changeSeverity(Severity severity) {
        requireEditable();
        this.severity = Objects.requireNonNull(severity);
    }

    private void transition(IncidentStatus expected, IncidentStatus target) {
        if (status != expected) {
            throw new InvalidIncidentTransitionException(status, target);
        }
        status = target;
    }

    private void requireEditable() {
        if (status == IncidentStatus.CLOSED) {
            throw new ResourceConflictException("Closed incidents cannot be edited.");
        }
    }
}
