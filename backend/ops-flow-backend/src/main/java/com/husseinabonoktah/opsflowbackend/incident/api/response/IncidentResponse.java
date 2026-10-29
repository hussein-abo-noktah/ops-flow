package com.husseinabonoktah.opsflowbackend.incident.api.response;

import com.husseinabonoktah.opsflowbackend.incident.domain.*;
import java.time.Instant;
import java.util.UUID;
public record IncidentResponse(UUID id, UUID organizationId, String incidentNumber,
        String title, String description, Severity severity, IncidentStatus status,
        UUID assignedTeamId, UUID assignedUserId, UUID createdBy,
        Instant acknowledgedAt, Instant mitigatedAt, Instant resolvedAt, Instant closedAt,
        Instant createdAt, Instant updatedAt, Long version) {}
