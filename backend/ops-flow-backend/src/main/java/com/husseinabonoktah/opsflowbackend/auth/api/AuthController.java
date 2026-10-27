package com.husseinabonoktah.opsflowbackend.auth.api;

import com.husseinabonoktah.opsflowbackend.auth.api.request.LoginRequest;
import com.husseinabonoktah.opsflowbackend.auth.api.request.RefreshTokenRequest;
import com.husseinabonoktah.opsflowbackend.auth.api.response.AuthResponse;
import com.husseinabonoktah.opsflowbackend.auth.application.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return tokens(service.login(request.email(), request.password(), http.getHeader("User-Agent"), http.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        return tokens(service.refresh(request.refreshToken(), http.getHeader("User-Agent"), http.getRemoteAddr()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        service.logout(request.refreshToken());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        service.logoutAll(UUID.fromString(Objects.requireNonNull(jwt.getSubject())));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "userId", Objects.requireNonNull(jwt.getSubject()), "organizationId", Objects.requireNonNull(jwt.getClaimAsString("org_id")),
                "authorities", Objects.requireNonNull(jwt.getClaimAsStringList("authorities"))));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> invalidCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials or token."));
    }

    private ResponseEntity<AuthResponse> tokens(AuthResponse response) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }
}
