package com.husseinabonoktah.opsflowbackend.user.application;

import com.husseinabonoktah.opsflowbackend.organization.domain.Organization;
import com.husseinabonoktah.opsflowbackend.organization.infrastructure.persistence.OrganizationRepository;
import com.husseinabonoktah.opsflowbackend.shared.exception.ResourceConflictException;
import com.husseinabonoktah.opsflowbackend.shared.exception.ResourceNotFoundException;
import com.husseinabonoktah.opsflowbackend.user.api.request.CreateUserRequest;
import com.husseinabonoktah.opsflowbackend.user.api.request.UpdateUserRequest;
import com.husseinabonoktah.opsflowbackend.user.api.response.UserResponse;
import com.husseinabonoktah.opsflowbackend.user.domain.User;
import com.husseinabonoktah.opsflowbackend.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository repository;
    private final OrganizationRepository organizationRepository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new ResourceConflictException("User email already exists: " + request.email());
        }
        Organization organization = findOrganization(request.organizationId());
        User user = mapper.toEntity(request, organization, passwordEncoder.encode(request.password()));
        return mapper.toResponse(repository.saveAndFlush(user));
    }

    public UserResponse getById(UUID id) {
        return mapper.toResponse(findById(id));
    }

    public Page<UserResponse> getAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("createdAt", "id")))
                .map(mapper::toResponse);
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = findById(id);
        if (repository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new ResourceConflictException("User email already exists: " + request.email());
        }
        mapper.updateEntity(user, request, findOrganization(request.organizationId()));
        return mapper.toResponse(repository.saveAndFlush(user));
    }

    private User findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private Organization findOrganization(UUID id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
    }
}
