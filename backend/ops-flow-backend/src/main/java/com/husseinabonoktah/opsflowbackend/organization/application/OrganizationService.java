package com.husseinabonoktah.opsflowbackend.organization.application;

import com.husseinabonoktah.opsflowbackend.organization.domain.Organization;
import com.husseinabonoktah.opsflowbackend.organization.api.request.CreateOrganizationRequest;
import com.husseinabonoktah.opsflowbackend.organization.api.response.OrganizationResponse;
import com.husseinabonoktah.opsflowbackend.organization.api.request.UpdateOrganizationRequest;
import com.husseinabonoktah.opsflowbackend.organization.infrastructure.persistence.OrganizationRepository;
import com.husseinabonoktah.opsflowbackend.shared.exception.ResourceConflictException;
import com.husseinabonoktah.opsflowbackend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationService {
    private final OrganizationRepository repository;
    private final OrganizationMapper mapper;

    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest request) {
        if (repository.existsBySlug(request.slug())) {
            throw new ResourceConflictException("Organization slug already exists: " + request.slug());
        }
        return mapper.toResponse(repository.saveAndFlush(mapper.toEntity(request)));
    }

    public OrganizationResponse getById(UUID id) {
        return mapper.toResponse(findById(id));
    }

    public Page<OrganizationResponse> getAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("createdAt", "id")))
                .map(mapper::toResponse);
    }

    @Transactional
    public OrganizationResponse update(UUID id, UpdateOrganizationRequest request) {
        Organization organization = findById(id);
        if (repository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new ResourceConflictException("Organization slug already exists: " + request.slug());
        }
        mapper.updateEntity(organization, request);
        return mapper.toResponse(repository.saveAndFlush(organization));
    }

    private Organization findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
    }
}
