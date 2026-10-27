package com.husseinabonoktah.opsflowbackend.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;

    public Duration accessTokenTtl() { return properties.accessTtl(); }

    public String generateAccessToken(AppUserPrincipal principal) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(properties.issuer()).audience(List.of(properties.audience()))
                .issuedAt(now).expiresAt(now.plus(accessTokenTtl())).id(UUID.randomUUID().toString())
                .subject(principal.getId().toString())
                .claim("org_id", principal.getOrganizationId().toString())
                .claim("token_type", "access")
                .claim("authorities", principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }
}
