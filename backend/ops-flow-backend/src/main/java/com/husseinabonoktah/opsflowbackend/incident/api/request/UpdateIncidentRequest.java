package com.husseinabonoktah.opsflowbackend.incident.api.request;

import jakarta.validation.constraints.*;
public record UpdateIncidentRequest(@NotBlank @Size(max = 250) String title,
        @Size(max = 10000) String description) {
    public UpdateIncidentRequest {
        title = title == null ? null : title.strip();
        description = description == null ? null : description.strip();
    }
}
