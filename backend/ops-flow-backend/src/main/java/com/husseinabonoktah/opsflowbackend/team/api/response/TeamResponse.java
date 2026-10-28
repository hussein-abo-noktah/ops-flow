package com.husseinabonoktah.opsflowbackend.team.api.response;

import com.husseinabonoktah.opsflowbackend.team.domain.TeamStatus;

import java.time.Instant;
import java.util.UUID;

public record TeamResponse(UUID id, UUID organizationId, String name, String code,
                           String description, TeamStatus status, Instant createdAt, Instant updatedAt) {
}
