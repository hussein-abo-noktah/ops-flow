package com.husseinabonoktah.opsflowbackend.incident.api.request;

import jakarta.validation.constraints.*;

public record EditIncidentCommentRequest(@NotBlank @Size(max = 10000) String comment,
                                         @NotNull @Min(0) Long version) {
}
