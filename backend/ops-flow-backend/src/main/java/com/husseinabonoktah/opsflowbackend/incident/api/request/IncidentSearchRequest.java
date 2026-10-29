package com.husseinabonoktah.opsflowbackend.incident.api.request;

import com.husseinabonoktah.opsflowbackend.incident.domain.IncidentStatus;
import com.husseinabonoktah.opsflowbackend.incident.domain.Severity;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

public record IncidentSearchRequest(
        @Size(max = 250) String q,
        IncidentStatus status,
        Severity severity,
        UUID assignedTeamId,
        UUID assignedUserId,
        UUID createdBy,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        @Pattern(regexp = "id|incidentNumber|title|severity|status|createdAt|updatedAt|acknowledgedAt|mitigatedAt|resolvedAt|closedAt")
        String sortBy,
        Sort.Direction direction
) {
    public IncidentSearchRequest {
        q = q == null || q.isBlank() ? null : q.strip();
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        sortBy = sortBy == null ? "createdAt" : sortBy;
        direction = direction == null ? Sort.Direction.ASC : direction;
    }

    @AssertTrue(message = "createdFrom must be before or equal to createdTo.")
    public boolean isDateRangeValid() {
        return createdFrom == null || createdTo == null || !createdFrom.isAfter(createdTo);
    }

    @AssertTrue(message = "The requested page offset is too large.")
    public boolean isOffsetValid() {
        return (long) page * size <= Integer.MAX_VALUE;
    }

    public Pageable toPageable() {
        Sort sort = Sort.by(direction, sortBy);
        if (!"id".equals(sortBy)) {
            sort = sort.and(Sort.by("id"));
        }
        return PageRequest.of(page, size, sort);
    }
}
