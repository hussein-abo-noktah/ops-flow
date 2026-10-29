package com.husseinabonoktah.opsflowbackend.incident.api.request;

import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;

public record HistoryPageRequest(@Min(0) Integer page, @Min(1) @Max(100) Integer size) {
    public HistoryPageRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }

    @AssertTrue(message = "The requested page offset is too large.")
    public boolean isOffsetValid() {
        return (long) page * size <= Integer.MAX_VALUE;
    }

    public Pageable toPageable(String orderBy) {
        return PageRequest.of(page, size, Sort.by(orderBy, "id"));
    }
}
