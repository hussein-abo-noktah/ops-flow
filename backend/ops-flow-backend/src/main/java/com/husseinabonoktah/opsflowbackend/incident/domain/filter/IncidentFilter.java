package com.husseinabonoktah.opsflowbackend.incident.domain.filter;

import com.husseinabonoktah.opsflowbackend.incident.domain.IncidentStatus;
import com.husseinabonoktah.opsflowbackend.incident.domain.Severity;

import java.time.Instant;
import java.util.UUID;

public record IncidentFilter(
        String q,
        IncidentStatus status,
        Severity severity,
        UUID assignedTeamId,
        UUID assignedUserId,
        UUID createdBy,
        Instant createdFrom,
        Instant createdTo
) {}
