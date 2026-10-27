package com.husseinabonoktah.opsflowbackend.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class JwtConfigTest {
    private final JwtConfig config = new JwtConfig();

    @Test
    void signsWithPrivateKeyAndVerifiesUsingOnlyThePublicKey() throws Exception {
        var properties = properties(pair(2048));
        var publicKey = config.jwtPublicKey(properties);
        var privateKey = config.jwtPrivateKey(properties, publicKey);
        var encoder = config.jwtEncoder(publicKey, privateKey);
        var decoder = config.jwtDecoder(publicKey, properties);
        var encoded = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims()));
        assertThat(encoded.getHeaders().get("alg").toString()).isEqualTo("RS256");
        assertThat(decoder.decode(encoded.getTokenValue()).getSubject()).isEqualTo(encoded.getSubject());

        var other = properties(pair(2048));
        assertThatThrownBy(() -> config.jwtDecoder(config.jwtPublicKey(other), other).decode(encoded.getTokenValue()))
                .isInstanceOf(JwtException.class);
        var rs512 = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS512).build(), claims()));
        assertThatThrownBy(() -> decoder.decode(rs512.getTokenValue())).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsMissingMalformedWeakAndMismatchedKeys() throws Exception {
        assertThatThrownBy(() -> config.jwtPublicKey(properties(null, null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.jwtPublicKey(properties(resource("not a PEM"), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.jwtPublicKey(properties(pair(1024))))
                .isInstanceOf(IllegalArgumentException.class);
        var valid = properties(pair(2048));
        var publicKey = config.jwtPublicKey(valid);
        assertThatThrownBy(() -> config.jwtPrivateKey(properties(valid.jwtPublicKey(), null), publicKey))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.jwtPrivateKey(properties(valid.jwtPublicKey(), resource("not a PEM")), publicKey))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.jwtPrivateKey(properties(pair(2048)), publicKey))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveOrSubsecondTokenLifetimes() {
        for (Duration ttl : List.of(Duration.ZERO, Duration.ofSeconds(-1), Duration.ofMillis(500))) {
            assertThatThrownBy(() -> new SecurityProperties(null, null, "issuer", "audience", ttl, Duration.ofDays(7), List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private JwtClaimsSet claims() {
        return JwtClaimsSet.builder().issuer("issuer").audience(List.of("audience"))
                .subject(UUID.randomUUID().toString()).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600))
                .id(UUID.randomUUID().toString()).claim("org_id", UUID.randomUUID().toString())
                .claim("token_type", "access").claim("authorities", List.of()).build();
    }
    private KeyPair pair(int bits) throws GeneralSecurityException {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(bits);
        return generator.generateKeyPair();
    }
    private Resource resource(String pem) { return new ByteArrayResource(pem.getBytes(StandardCharsets.US_ASCII)); }
    private SecurityProperties properties(KeyPair pair) {
        return properties(resource(TestRsaKeys.pem("PUBLIC KEY", pair.getPublic().getEncoded())),
                resource(TestRsaKeys.pem("PRIVATE KEY", pair.getPrivate().getEncoded())));
    }
    private SecurityProperties properties(Resource publicKey, Resource privateKey) {
        return new SecurityProperties(publicKey, privateKey, "issuer", "audience", Duration.ofMinutes(10), Duration.ofDays(7), List.of());
    }
}
