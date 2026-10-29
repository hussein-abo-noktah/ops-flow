package com.husseinabonoktah.opsflowbackend.incident.api;

import com.husseinabonoktah.opsflowbackend.incident.api.request.*;
import com.husseinabonoktah.opsflowbackend.incident.api.response.*;
import com.husseinabonoktah.opsflowbackend.incident.application.IncidentCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents/{incidentId}/comments")
@RequiredArgsConstructor
public class IncidentCommentController {
    private final IncidentCommentService service;

    @PostMapping
    @PreAuthorize("hasAuthority('incident:update') or hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<IncidentCommentResponse> create(@PathVariable UUID incidentId,
            @Valid @RequestBody CreateIncidentCommentRequest request) {
        var response = service.create(incidentId, request);
        return ResponseEntity.created(URI.create("/api/incidents/" + incidentId + "/comments/" + response.id())).body(response);
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("hasAuthority('incident:update') or hasRole('PLATFORM_ADMIN')")
    public IncidentCommentResponse edit(@PathVariable UUID incidentId, @PathVariable UUID commentId,
            @Valid @RequestBody EditIncidentCommentRequest request) {
        return service.edit(incidentId, commentId, request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('incident:view') or hasRole('PLATFORM_ADMIN')")
    public PagedModel<IncidentCommentResponse> getAll(@PathVariable UUID incidentId,
            @Valid @ModelAttribute HistoryPageRequest request) {
        return new PagedModel<>(service.getAll(incidentId, request));
    }

    @GetMapping("/{commentId}")
    @PreAuthorize("hasAuthority('incident:view') or hasRole('PLATFORM_ADMIN')")
    public IncidentCommentResponse getById(@PathVariable UUID incidentId, @PathVariable UUID commentId) {
        return service.getById(incidentId, commentId);
    }

    @GetMapping("/{commentId}/history")
    @PreAuthorize("hasAuthority('incident:view') or hasRole('PLATFORM_ADMIN')")
    public PagedModel<IncidentCommentRevisionResponse> history(@PathVariable UUID incidentId, @PathVariable UUID commentId,
            @Valid @ModelAttribute HistoryPageRequest request) {
        return new PagedModel<>(service.history(incidentId, commentId, request));
    }
}
