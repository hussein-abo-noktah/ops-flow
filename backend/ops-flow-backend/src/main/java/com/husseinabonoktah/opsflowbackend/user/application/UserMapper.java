package com.husseinabonoktah.opsflowbackend.user.application;

import com.husseinabonoktah.opsflowbackend.organization.domain.Organization;
import com.husseinabonoktah.opsflowbackend.user.api.request.CreateUserRequest;
import com.husseinabonoktah.opsflowbackend.user.api.request.UpdateUserRequest;
import com.husseinabonoktah.opsflowbackend.user.api.response.UserResponse;
import com.husseinabonoktah.opsflowbackend.user.domain.User;
import com.husseinabonoktah.opsflowbackend.user.domain.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toEntity(CreateUserRequest request, Organization organization, String passwordHash) {
        User user = new User();
        user.setOrganization(organization);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordHash);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    public void updateEntity(User user, UpdateUserRequest request, Organization organization) {
        user.setOrganization(organization);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setStatus(request.status());
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getOrganization().getId(), user.getFirstName(),
                user.getLastName(), user.getEmail(), user.getStatus(), user.getLastLoginAt(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
