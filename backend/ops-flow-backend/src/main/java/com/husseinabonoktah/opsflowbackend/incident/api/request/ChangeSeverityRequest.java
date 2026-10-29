package com.husseinabonoktah.opsflowbackend.incident.api.request;

import jakarta.validation.constraints.NotNull;
import com.husseinabonoktah.opsflowbackend.incident.domain.Severity;
public record ChangeSeverityRequest(@NotNull Severity severity) {}
