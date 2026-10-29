package com.husseinabonoktah.opsflowbackend.incident.api.request;

import com.husseinabonoktah.opsflowbackend.incident.domain.TimelineEventType;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.Instant;
import java.util.UUID;

public record IncidentTimelineQuery(TimelineEventType eventType, UUID actorId, UUID commentId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @Min(0) Integer page, @Min(1) @Max(100) Integer size, Sort.Direction direction) {
    public IncidentTimelineQuery {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        direction = direction == null ? Sort.Direction.ASC : direction;
    }

    @AssertTrue(message = "from must be before or equal to to.")
    public boolean isDateRangeValid() {
        return from == null || to == null || !from.isAfter(to);
    }

    @AssertTrue(message = "The requested page offset is too large.")
    public boolean isOffsetValid() {
        return (long) page * size <= Integer.MAX_VALUE;
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(direction, "createdAt").and(Sort.by(direction, "id")));
    }
}
