package com.husseinabonoktah.opsflowbackend.organization.api.response;

import com.husseinabonoktah.opsflowbackend.organization.domain.OrganizationStatus;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id, String name, String slug, OrganizationStatus status,
        Instant createdAt, Instant updatedAt
) {
}
