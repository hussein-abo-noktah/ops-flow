package com.husseinabonoktah.opsflowbackend.team.api;

import com.husseinabonoktah.opsflowbackend.team.api.request.AddTeamMemberRequest;
import com.husseinabonoktah.opsflowbackend.team.api.response.TeamMemberResponse;
import com.husseinabonoktah.opsflowbackend.team.application.TeamMemberService;
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
@RequestMapping("/api/teams/{teamId}/members")
@RequiredArgsConstructor
public class TeamMemberController {
    private final TeamMemberService service;

    @PostMapping
    @PreAuthorize("hasAuthority('team:update') or hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<TeamMemberResponse> add(@PathVariable UUID teamId,
                                                 @Valid @RequestBody AddTeamMemberRequest request) {
        var response = service.add(teamId, request);
        return ResponseEntity.created(URI.create("/api/teams/" + teamId + "/members/" + response.userId()))
                .body(response);
    }

    @GetMapping
    public PagedModel<TeamMemberResponse> getAll(@PathVariable UUID teamId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new PagedModel<>(service.getAll(teamId, page, size));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('team:update') or hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Void> remove(@PathVariable UUID teamId, @PathVariable UUID userId) {
        service.remove(teamId, userId);
        return ResponseEntity.noContent().build();
    }
}
