package com.husseinabonoktah.opsflowbackend.incident.api.response;

import com.husseinabonoktah.opsflowbackend.incident.domain.TimelineEventType;
import java.time.Instant;
import java.util.UUID;
public record IncidentTimelineResponse(UUID id, UUID incidentId, TimelineEventType eventType,
        UUID actorId, String oldValue, String newValue, String metadata, Instant createdAt, UUID commentId) {}
