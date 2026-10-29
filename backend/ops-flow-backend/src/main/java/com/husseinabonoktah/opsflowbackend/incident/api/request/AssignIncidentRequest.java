package com.husseinabonoktah.opsflowbackend.incident.api.request;

import java.util.UUID;
public record AssignIncidentRequest(UUID teamId, UUID userId) {}
