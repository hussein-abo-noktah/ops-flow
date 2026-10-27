package com.husseinabonoktah.opsflowbackend.auth.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Configuration
public class JwtConfig {
    @Bean
    RSAPublicKey jwtPublicKey(SecurityProperties properties) {
        Resource resource = properties.jwtPublicKey();
        if (resource == null) throw new IllegalArgumentException("AUTH_JWT_PUBLIC_KEY must point to an X.509 PEM public key");
        try (var input = resource.getInputStream()) {
            RSAPublicKey key = RsaKeyConverters.x509().convert(input);
            if (key.getModulus().bitLength() < 2048) {
                throw new IllegalArgumentException("JWT RSA public key must be at least 2048 bits");
            }
            return key;
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Cannot load AUTH_JWT_PUBLIC_KEY; use an X.509 PEM RSA key of at least 2048 bits", exception);
        }
    }

    @Bean
    RSAPrivateKey jwtPrivateKey(SecurityProperties properties, RSAPublicKey publicKey) {
        Resource resource = properties.jwtPrivateKey();
        if (resource == null) throw new IllegalArgumentException("AUTH_JWT_PRIVATE_KEY must point to a PKCS#8 PEM private key");
        try (var input = resource.getInputStream()) {
            RSAPrivateKey key = RsaKeyConverters.pkcs8().convert(input);
            if (key.getModulus().bitLength() < 2048) {
                throw new IllegalArgumentException("JWT RSA private key must be at least 2048 bits");
            }
            byte[] challenge = "ops-flow-key-pair-check".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(challenge);
            byte[] signed = signature.sign();
            signature.initVerify(publicKey);
            signature.update(challenge);
            if (!signature.verify(signed)) throw new IllegalArgumentException("JWT RSA public and private keys do not match");
            return key;
        } catch (IOException | GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Cannot load AUTH_JWT_PRIVATE_KEY; use a matching PKCS#8 PEM RSA key of at least 2048 bits", exception);
        }
    }

    @Bean
    JwtEncoder jwtEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        var jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    @Bean
    JwtDecoder jwtDecoder(RSAPublicKey publicKey, SecurityProperties properties) {
        var decoder = NimbusJwtDecoder.withPublicKey(publicKey).signatureAlgorithm(SignatureAlgorithm.RS256).build();
        OAuth2TokenValidator<Jwt> claimsValidator = jwt -> {
            try {
                UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
                UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("org_id")));
                var authorities = jwt.getClaimAsStringList("authorities");
                if (jwt.getExpiresAt() != null && jwt.getIssuedAt() != null && jwt.getId() != null
                        && authorities != null && authorities.stream().noneMatch(String::isBlank)
                        && Objects.requireNonNull(jwt.getAudience()).contains(properties.audience())
                        && "access".equals(jwt.getClaimAsString("token_type"))) {
                    return OAuth2TokenValidatorResult.success();
                }
            } catch (IllegalArgumentException | NullPointerException | ClassCastException exception) {
                // Invalid identity claims are authentication failures.
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid access token claims", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ZERO),
                new JwtIssuerValidator(properties.issuer()), claimsValidator));
        return decoder;
    }
}
