package com.husseinabonoktah.opsflowbackend.auth.security;

import org.springframework.test.context.DynamicPropertyRegistry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

/** Ephemeral test keys, unrelated to the deployment signing key. */
public final class TestRsaKeys {
    private static final Path PUBLIC_KEY;
    private static final Path PRIVATE_KEY;
    static {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var pair = generator.generateKeyPair();
            Path directory = Files.createTempDirectory("ops-flow-test-keys-");
            directory.toFile().deleteOnExit();
            PUBLIC_KEY = directory.resolve("public.pem");
            PRIVATE_KEY = directory.resolve("private.pem");
            Files.writeString(PUBLIC_KEY, pem("PUBLIC KEY", pair.getPublic().getEncoded()), StandardCharsets.US_ASCII);
            Files.writeString(PRIVATE_KEY, pem("PRIVATE KEY", pair.getPrivate().getEncoded()), StandardCharsets.US_ASCII);
            PUBLIC_KEY.toFile().deleteOnExit();
            PRIVATE_KEY.toFile().deleteOnExit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
    private TestRsaKeys() {}

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt-public-key", () -> PUBLIC_KEY.toUri().toString());
        registry.add("app.security.jwt-private-key", () -> PRIVATE_KEY.toUri().toString());
    }

    static String pem(String label, byte[] encoded) {
        return "-----BEGIN " + label + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded)
                + "\n-----END " + label + "-----\n";
    }
}
