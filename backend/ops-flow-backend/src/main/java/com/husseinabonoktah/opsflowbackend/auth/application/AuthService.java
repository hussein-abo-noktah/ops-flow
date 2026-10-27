package com.husseinabonoktah.opsflowbackend.auth.application;

import com.husseinabonoktah.opsflowbackend.auth.api.response.AuthResponse;
import com.husseinabonoktah.opsflowbackend.auth.security.AppUserPrincipal;
import com.husseinabonoktah.opsflowbackend.auth.security.JwtService;
import com.husseinabonoktah.opsflowbackend.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = BadCredentialsException.class)
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse login(String email, String password, String userAgent, String ipAddress) {
        AppUserPrincipal principal;
        try {
            var authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, password));
            principal = (AppUserPrincipal) authentication.getPrincipal();
        } catch (AuthenticationException exception) {
            throw new BadCredentialsException("Invalid credentials or token.");
        }
        assert principal != null;
        var user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials or token."));
        user.setLastLoginAt(Instant.now());
        return response(principal, refreshTokenService.create(user, userAgent, ipAddress));
    }

    public AuthResponse refresh(String token, String userAgent, String ipAddress) {
        var rotation = refreshTokenService.rotate(token, userAgent, ipAddress);
        return response(new AppUserPrincipal(rotation.user()), rotation.rawToken());
    }

    public void logout(String token) { refreshTokenService.revoke(token); }
    public void logoutAll(UUID userId) { refreshTokenService.revokeAll(userId); }

    private AuthResponse response(AppUserPrincipal principal, String refreshToken) {
        return new AuthResponse(jwtService.generateAccessToken(principal), refreshToken,
                "Bearer", jwtService.accessTokenTtl().toSeconds());
    }
}
