package com.husseinabonoktah.opsflowbackend.auth.api.response;

public record AuthResponse(

        String accessToken,

        String refreshToken,

        String tokenType,

        long expiresIn

) {
}