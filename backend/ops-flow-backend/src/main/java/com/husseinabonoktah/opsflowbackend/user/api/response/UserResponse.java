package com.husseinabonoktah.opsflowbackend.user.api.response;

import com.husseinabonoktah.opsflowbackend.user.domain.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id, UUID organizationId, String firstName, String lastName, String email,
        UserStatus status, Instant lastLoginAt, Instant createdAt, Instant updatedAt
) {
}
