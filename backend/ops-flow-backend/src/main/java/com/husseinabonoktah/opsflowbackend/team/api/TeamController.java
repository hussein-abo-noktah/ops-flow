package com.husseinabonoktah.opsflowbackend.team.api;

import com.husseinabonoktah.opsflowbackend.team.api.request.*;
import com.husseinabonoktah.opsflowbackend.team.api.response.TeamResponse;
import com.husseinabonoktah.opsflowbackend.team.application.TeamService;
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
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService service;

    @PostMapping
    @PreAuthorize("hasAuthority('team:create') or hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest request) {
        TeamResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/teams/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public TeamResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping
    public PagedModel<TeamResponse> getAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new PagedModel<>(service.getAll(page, size));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('team:update') or hasRole('PLATFORM_ADMIN')")
    public TeamResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTeamRequest request) {
        return service.update(id, request);
    }
}
