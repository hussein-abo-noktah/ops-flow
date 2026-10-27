package com.husseinabonoktah.opsflowbackend.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import java.time.Duration;
import java.util.List;

@ConfigurationProperties("app.security")
public record SecurityProperties(Resource jwtPublicKey, Resource jwtPrivateKey, String issuer, String audience,
                                 Duration accessTtl, Duration refreshTtl, List<String> allowedOrigins) {
    public SecurityProperties {
        if (issuer == null || issuer.isBlank() || audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("JWT issuer and audience are required");
        }
        if (accessTtl == null || accessTtl.compareTo(Duration.ofSeconds(1)) < 0
                || refreshTtl == null || refreshTtl.compareTo(Duration.ofSeconds(1)) < 0) {
            throw new IllegalArgumentException("Token lifetimes must be at least one second");
        }
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
