package com.husseinabonoktah.opsflowbackend.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class CurrentUser {

    public UUID id() {

        Authentication auth =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assert auth != null;
        Jwt jwt =
                (Jwt) auth.getPrincipal();

        assert jwt != null;
        return UUID.fromString(
                Objects.requireNonNull(jwt.getSubject())
        );
    }

    public UUID organizationId() {

        Authentication auth =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        Jwt jwt =
                (Jwt) auth.getPrincipal();

        return UUID.fromString(
                jwt.getClaimAsString("org_id")
        );
    }
}
