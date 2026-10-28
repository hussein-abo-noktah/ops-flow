package com.husseinabonoktah.opsflowbackend.team.api.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddTeamMemberRequest(@NotNull UUID userId, boolean primary) {
}
