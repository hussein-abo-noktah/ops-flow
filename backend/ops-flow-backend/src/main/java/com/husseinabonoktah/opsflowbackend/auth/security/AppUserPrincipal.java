package com.husseinabonoktah.opsflowbackend.auth.security;

import com.husseinabonoktah.opsflowbackend.auth.domain.Permission;
import com.husseinabonoktah.opsflowbackend.auth.domain.Role;
import com.husseinabonoktah.opsflowbackend.user.domain.User;
import com.husseinabonoktah.opsflowbackend.user.domain.UserStatus;
import com.husseinabonoktah.opsflowbackend.organization.domain.OrganizationStatus;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class AppUserPrincipal implements UserDetails {
    private final UUID id;
    private final UUID organizationId;
    private final String email;
    private final String password;
    private final UserStatus status;
    private final boolean organizationActive;
    private final Set<String> roles;
    private final Set<String> permissions;

    public AppUserPrincipal(User user) {

        this.id = user.getId();

        this.organizationId =
                user.getOrganization().getId();

        this.email = user.getEmail();

        this.password = user.getPasswordHash();

        this.status = user.getStatus();
        this.organizationActive = user.getOrganization().getStatus() == OrganizationStatus.ACTIVE;

        var organizationRoles = user.getRoles().stream()
                .filter(role -> organizationId.equals(role.getOrganization().getId()))
                .toList();

        this.roles =
                organizationRoles.stream()
                        .filter(role -> !"PLATFORM_ADMIN".equals(role.getCode()) || role.isSystem())
                        .map(Role::getCode)
                        .collect(Collectors.toSet());

        this.permissions =
                organizationRoles.stream()
                        .flatMap(role ->
                                role.getPermissions().stream()
                        )
                        .map(Permission::getCode)
                        .collect(Collectors.toSet());
    }

    @Override
    @NonNull
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE && organizationActive;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority>
    getAuthorities() {

        Set<GrantedAuthority> authorities =
                new HashSet<>();

        permissions.forEach(permission ->
                authorities.add(
                        new SimpleGrantedAuthority(permission)
                )
        );

        roles.forEach(role ->
                authorities.add(
                        new SimpleGrantedAuthority(
                                "ROLE_" + role
                        )
                )
        );

        return authorities;
    }
}
