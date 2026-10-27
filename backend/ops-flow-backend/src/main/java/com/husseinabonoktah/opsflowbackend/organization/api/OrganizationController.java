package com.husseinabonoktah.opsflowbackend.organization.api;

import com.husseinabonoktah.opsflowbackend.organization.api.request.CreateOrganizationRequest;
import com.husseinabonoktah.opsflowbackend.organization.api.response.OrganizationResponse;
import com.husseinabonoktah.opsflowbackend.organization.api.request.UpdateOrganizationRequest;
import com.husseinabonoktah.opsflowbackend.organization.application.OrganizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationService service;

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request) {
        OrganizationResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/organizations/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public OrganizationResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping
    public PagedModel<OrganizationResponse> getAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new PagedModel<>(service.getAll(page, size));
    }

    @PutMapping("/{id}")
    public OrganizationResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody UpdateOrganizationRequest request) {
        return service.update(id, request);
    }
}
