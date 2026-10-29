package com.husseinabonoktah.opsflowbackend.incident.api.request;

import jakarta.validation.constraints.*;
import com.husseinabonoktah.opsflowbackend.incident.domain.Severity;

public record CreateIncidentRequest(@NotBlank @Size(max = 250) String title,
                                    @Size(max = 10000) String description, @NotNull Severity severity) {
    public CreateIncidentRequest {
        title = title == null ? null : title.strip();
    }
}
