package com.husseinabonoktah.opsflowbackend.incident.api.request;

import jakarta.validation.constraints.*;

public record CreateIncidentCommentRequest(@NotBlank @Size(max = 10000) String comment) {
    public CreateIncidentCommentRequest {
        comment = comment == null ? null : comment.strip();
    }
}
