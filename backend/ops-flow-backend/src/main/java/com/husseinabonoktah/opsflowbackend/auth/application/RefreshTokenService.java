package com.husseinabonoktah.opsflowbackend.auth.application;

import com.husseinabonoktah.opsflowbackend.auth.domain.RefreshToken;
import com.husseinabonoktah.opsflowbackend.auth.infrastructure.persistence.RefreshTokenRepository;
import com.husseinabonoktah.opsflowbackend.auth.security.AppUserPrincipal;
import com.husseinabonoktah.opsflowbackend.auth.security.RefreshTokenGenerator;
import com.husseinabonoktah.opsflowbackend.auth.security.RefreshTokenHasher;
import com.husseinabonoktah.opsflowbackend.auth.security.SecurityProperties;
import com.husseinabonoktah.opsflowbackend.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = BadCredentialsException.class)
public class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final RefreshTokenGenerator generator;
    private final RefreshTokenHasher hasher;
    private final SecurityProperties properties;
    public record Rotation(User user, String rawToken) {}

    public String create(User user, String userAgent, String ipAddress) {
        String raw = generator.generate();
        repository.save(new RefreshToken(user, hasher.hash(raw), Instant.now().plus(properties.refreshTtl()), userAgent, ipAddress));
        return raw;
    }

    public Rotation rotate(String raw, String userAgent, String ipAddress) {
        RefreshToken token = find(raw);
        if (token.isRevoked()) {
            // Retain consumed hashes so a replay can revoke all outstanding refresh credentials.
            revokeAll(token.getUser().getId());
            throw invalid();
        }
        if (token.isExpired() || !new AppUserPrincipal(token.getUser()).isEnabled()) {
            token.revoke();
            throw invalid();
        }
        String next = generator.generate();
        // Rotation retains the original absolute expiry instead of extending it indefinitely.
        var replacement = repository.save(new RefreshToken(token.getUser(), hasher.hash(next),
                token.getExpiresAt(), userAgent, ipAddress));
        token.replaceWith(replacement);
        return new Rotation(token.getUser(), next);
    }

    public void revoke(String raw) {
        if (raw == null || raw.isBlank()) return;
        repository.findByTokenHashForUpdate(hasher.hash(raw)).ifPresent(RefreshToken::revoke);
    }

    public void revokeAll(UUID userId) { repository.revokeAllByUserId(userId, Instant.now()); }

    private RefreshToken find(String raw) {
        if (raw == null || raw.isBlank()) throw invalid();
        return repository.findByTokenHashForUpdate(hasher.hash(raw)).orElseThrow(this::invalid);
    }
    private BadCredentialsException invalid() { return new BadCredentialsException("Invalid credentials or token."); }
}
