package com.husseinabonoktah.opsflowbackend.team.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;


public record CreateTeamRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        @Size(max = 50)
        String code,

        @Size(max = 1000)
        String description

) {
    public CreateTeamRequest {
        name = name == null ? null : name.strip();
        code = code == null ? null : code.strip().toUpperCase(Locale.ROOT);
        description = description == null ? null : description.strip();
    }
}
