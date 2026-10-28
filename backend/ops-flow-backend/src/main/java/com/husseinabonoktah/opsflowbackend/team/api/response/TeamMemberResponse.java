package com.husseinabonoktah.opsflowbackend.team.api.response;

import java.time.Instant;
import java.util.UUID;

public record TeamMemberResponse(UUID id, UUID teamId, UUID userId, String firstName,
                                 String lastName, String email, boolean primary, Instant joinedAt) {
}
