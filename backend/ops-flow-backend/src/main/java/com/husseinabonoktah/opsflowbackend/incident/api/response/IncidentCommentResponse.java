package com.husseinabonoktah.opsflowbackend.incident.api.response;

import java.time.Instant;
import java.util.UUID;

public record IncidentCommentResponse(UUID id, UUID incidentId, UUID userId, String comment,
        boolean edited, Long version, Instant createdAt, Instant updatedAt) {}
