package com.husseinabonoktah.opsflowbackend.auth.security;

import com.husseinabonoktah.opsflowbackend.user.domain.User;
import com.husseinabonoktah.opsflowbackend.user.domain.UserStatus;
import com.husseinabonoktah.opsflowbackend.organization.domain.OrganizationStatus;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Getter
public class AppUserPrincipal implements UserDetails {
    private final UUID id;
    private final UUID organizationId;
    private final String email;
    private final String password;
    private final UserStatus status;
    private final boolean organizationActive;
    private final List<GrantedAuthority> authorities;

    public AppUserPrincipal(User user) {
        id = user.getId();
        organizationId = user.getOrganization().getId();
        email = user.getEmail();
        password = user.getPasswordHash();
        status = user.getStatus();
        organizationActive = user.getOrganization().getStatus() == OrganizationStatus.ACTIVE;
        Set<String> codes = new TreeSet<>();
        user.getRoles().stream().filter(role -> organizationId.equals(role.getOrganization().getId())).forEach(role -> {
            if (!"PLATFORM_ADMIN".equals(role.getCode()) || role.isSystem()) {
                codes.add("ROLE_" + role.getCode());
            }
            role.getPermissions().forEach(permission -> codes.add("PERM_" + permission.getCode()));
        });
        authorities = codes.stream().<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
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
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
