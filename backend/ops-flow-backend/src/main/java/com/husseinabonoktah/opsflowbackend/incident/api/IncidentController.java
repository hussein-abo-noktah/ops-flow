package com.husseinabonoktah.opsflowbackend.incident.api;

import com.husseinabonoktah.opsflowbackend.incident.api.request.*;
import com.husseinabonoktah.opsflowbackend.incident.api.response.*;
import com.husseinabonoktah.opsflowbackend.incident.application.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService service;
    private final IncidentTimelineService timeline;

    @PostMapping
    @PreAuthorize("hasAuthority('incident:create') or hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody CreateIncidentRequest request) {
        IncidentResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/incidents/" + response.id())).body(response);
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('incident:view') or hasRole('PLATFORM_ADMIN')")
    public IncidentResponse getById(@PathVariable UUID id) { return service.getById(id); }
    @GetMapping
    @PreAuthorize("hasAuthority('incident:view') or hasRole('PLATFORM_ADMIN')")
    public PagedModel<IncidentResponse> getAll(@Valid @ModelAttribute IncidentSearchRequest request) {
        return new PagedModel<>(service.search(request));
    }
    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAuthority('incident:view') or hasRole('PLATFORM_ADMIN')")
    public PagedModel<IncidentTimelineResponse> timeline(@PathVariable UUID id,
            @Valid @ModelAttribute IncidentTimelineQuery request) {
        return new PagedModel<>(timeline.search(id, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('incident:update') or hasRole('PLATFORM_ADMIN')")
    public IncidentResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateIncidentRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('incident:assign') or hasRole('PLATFORM_ADMIN')")
    public IncidentResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignIncidentRequest request) {
        return service.assign(id, request);
    }

    @PostMapping("/{id}/severity")
    @PreAuthorize("hasAuthority('incident:update') or hasRole('PLATFORM_ADMIN')")
    public IncidentResponse changeSeverity(@PathVariable UUID id, @Valid @RequestBody ChangeSeverityRequest request) {
        return service.changeSeverity(id, request);
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAuthority('incident:update') or hasRole('PLATFORM_ADMIN')")
    public IncidentResponse acknowledge(@PathVariable UUID id) {
        return service.acknowledge(id);
    }

    @PostMapping("/{id}/start-investigation")
    @PreAuthorize("hasAuthority('incident:update') or hasRole('PLATFORM_ADMIN')")
    public IncidentResponse startInvestigation(@PathVariable UUID id) {
        return service.startInvestigation(id);
    }

    @PostMapping("/{id}/mitigate")
    @PreAuthorize("hasAuthority('incident:update') or hasRole('PLATFORM_ADMIN')")
    public IncidentResponse mitigate(@PathVariable UUID id) {
        return service.mitigate(id);
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('incident:resolve') or hasRole('PLATFORM_ADMIN')")
    public IncidentResponse resolve(@PathVariable UUID id) {
        return service.resolve(id);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('incident:resolve') or hasRole('PLATFORM_ADMIN')")
    public IncidentResponse close(@PathVariable UUID id) {
        return service.close(id);
    }
}
