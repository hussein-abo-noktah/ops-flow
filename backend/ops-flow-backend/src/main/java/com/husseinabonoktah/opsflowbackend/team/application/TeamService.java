package com.husseinabonoktah.opsflowbackend.team.application;

import com.husseinabonoktah.opsflowbackend.organization.infrastructure.persistence.OrganizationRepository;
import com.husseinabonoktah.opsflowbackend.shared.exception.*;
import com.husseinabonoktah.opsflowbackend.shared.security.CurrentUser;
import com.husseinabonoktah.opsflowbackend.team.api.request.*;
import com.husseinabonoktah.opsflowbackend.team.api.response.TeamResponse;
import com.husseinabonoktah.opsflowbackend.team.domain.Team;
import com.husseinabonoktah.opsflowbackend.team.infrastructure.persistence.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {
    private final TeamRepository repository;
    private final OrganizationRepository organizations;
    private final CurrentUser currentUser;
    private final TeamMapper mapper;

    @Transactional
    public TeamResponse create(CreateTeamRequest request) {
        UUID organizationId = currentUser.organizationId();
        if (repository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, request.code())) {
            throw new ResourceConflictException("Team code already exists: " + request.code());
        }
        var organization = organizations.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        return mapper.toResponse(repository.saveAndFlush(mapper.toEntity(request, organization)));
    }

    public TeamResponse getById(UUID id) {
        return mapper.toResponse(findById(id));
    }

    public Page<TeamResponse> getAll(int page, int size) {
        return repository.findAllByOrganizationId(currentUser.organizationId(),
                PageRequest.of(page, size, Sort.by("createdAt", "id"))).map(mapper::toResponse);
    }

    @Transactional
    public TeamResponse update(UUID id, UpdateTeamRequest request) {
        Team team = findById(id);
        if (repository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(
                currentUser.organizationId(), request.code(), id)) {
            throw new ResourceConflictException("Team code already exists: " + request.code());
        }
        mapper.updateEntity(team, request);
        return mapper.toResponse(repository.saveAndFlush(team));
    }

    public Team findById(UUID id) {
        return repository.findByIdAndOrganizationId(id, currentUser.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
    }
}
