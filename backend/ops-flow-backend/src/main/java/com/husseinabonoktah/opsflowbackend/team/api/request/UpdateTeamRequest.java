package com.husseinabonoktah.opsflowbackend.team.api.request;

import com.husseinabonoktah.opsflowbackend.team.domain.TeamStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record UpdateTeamRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 50) String code,
        @Size(max = 1000) String description,
        @NotNull TeamStatus status
) {
    public UpdateTeamRequest {
        name = name == null ? null : name.strip();
        code = code == null ? null : code.strip().toUpperCase(Locale.ROOT);
        description = description == null ? null : description.strip();
    }
}
