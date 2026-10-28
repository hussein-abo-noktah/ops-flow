package com.husseinabonoktah.opsflowbackend.auth;

import com.husseinabonoktah.opsflowbackend.auth.api.AuthController;
import com.husseinabonoktah.opsflowbackend.auth.application.*;
import com.husseinabonoktah.opsflowbackend.auth.security.*;
import com.husseinabonoktah.opsflowbackend.organization.domain.*;
import com.husseinabonoktah.opsflowbackend.organization.infrastructure.persistence.OrganizationRepository;
import com.husseinabonoktah.opsflowbackend.shared.exception.GlobalExceptionHandler;
import com.husseinabonoktah.opsflowbackend.shared.security.PasswordConfig;
import com.husseinabonoktah.opsflowbackend.user.domain.*;
import com.husseinabonoktah.opsflowbackend.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AuthIntegrationIT.AuthTestApplication.class)
@AutoConfigureMockMvc
class AuthIntegrationIT {
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.husseinabonoktah.opsflowbackend")
    @EnableJpaRepositories(basePackages = {
            "com.husseinabonoktah.opsflowbackend.auth.infrastructure.persistence",
            "com.husseinabonoktah.opsflowbackend.user.infrastructure.persistence",
            "com.husseinabonoktah.opsflowbackend.organization.infrastructure.persistence"})
    @Import({AuthController.class, AuthService.class, RefreshTokenService.class, AppUserDetailsService.class,
            JwtService.class, JwtConfig.class, SecurityConfig.class, RefreshTokenGenerator.class,
            RefreshTokenHasher.class, PasswordConfig.class, GlobalExceptionHandler.class})
    static class AuthTestApplication {
    }

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        TestRsaKeys.register(registry);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository users;
    @Autowired
    OrganizationRepository organizations;
    @Autowired
    PasswordEncoder passwords;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    JwtDecoder decoder;
    @Autowired
    JwtEncoder encoder;
    private final ObjectMapper json = new ObjectMapper();
    private User user;

    @BeforeEach
    void setup() {
        jdbc.execute("TRUNCATE organizations CASCADE");
        var org = new Organization();
        org.setName("Auth tests");
        org.setSlug("auth-tests");
        org.setStatus(OrganizationStatus.ACTIVE);
        org = organizations.saveAndFlush(org);
        user = new User();
        user.setOrganization(org);
        user.setFirstName("Alice");
        user.setLastName("Ops");
        user.setEmail("alice@example.com");
        user.setPasswordHash(passwords.encode("secure-password"));
        user.setStatus(UserStatus.ACTIVE);
        user = users.saveAndFlush(user);
    }

    @Test
    void loginIsPublicAndReturnsTokensWithMatchingClaimsAndTtl() throws Exception {
        var result = login(200);
        assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
        assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
        var body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("tokenType").asString()).isEqualTo("Bearer");
        var jwt = decoder.decode(field(result, "accessToken"));
        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsString("org_id")).isEqualTo(user.getOrganization().getId().toString());
        assert jwt.getExpiresAt() != null;
        assert jwt.getIssuedAt() != null;
        assertThat(jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond()).isEqualTo(body.get("expiresIn").asLong());
        assertThat(users.findById(user.getId()).orElseThrow().getLastLoginAt()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT token_hash FROM refresh_tokens", String.class)).hasSize(64).isNotEqualTo(field(result, "refreshToken"));
        me(field(result, "accessToken"), 200);
    }

    @Test
    void refreshRotatesAndReplayRevokesOutstandingRefreshTokens() throws Exception {
        var original = login(200);
        var independent = login(200);
        var rotated = tokenRequest("refresh", field(original, "refreshToken"), 200);
        assertThat(field(rotated, "refreshToken")).isNotEqualTo(field(original, "refreshToken"));
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT expires_at) FROM refresh_tokens WHERE id IN (SELECT id FROM refresh_tokens WHERE replaced_by IS NOT NULL) OR id IN (SELECT replaced_by FROM refresh_tokens WHERE replaced_by IS NOT NULL)", Integer.class)).isEqualTo(1);
        tokenRequest("refresh", field(original, "refreshToken"), 401);
        tokenRequest("refresh", field(rotated, "refreshToken"), 401);
        tokenRequest("refresh", field(independent, "refreshToken"), 401);
    }

    @Test
    void concurrentRefreshConsumesTheOriginalOnlyOnce() throws Exception {
        String raw = field(login(200), "refreshToken");
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<MvcResult> rotate = () -> {
                barrier.await(10, TimeUnit.SECONDS);
                return tokenRequest("refresh", raw, null);
            };
            var one = executor.submit(rotate);
            var two = executor.submit(rotate);
            assertThat(List.of(one.get(20, TimeUnit.SECONDS).getResponse().getStatus(),
                    two.get(20, TimeUnit.SECONDS).getResponse().getStatus())).containsExactlyInAnyOrder(200, 401);
        }
    }

    @Test
    void logoutRevokesRefreshAndLogoutAllRequiresBearer() throws Exception {
        var one = login(200);
        var two = login(200);
        tokenRequest("logout", field(one, "refreshToken"), 204);
        tokenRequest("logout", field(one, "refreshToken"), 204);
        // JWTs are stateless and remain valid until expiry after refresh-token revocation.
        me(field(one, "accessToken"), 200);
        mvc.perform(post("/api/auth/logout-all")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout-all").header("Authorization", "Bearer " + field(two, "accessToken")))
                .andExpect(status().isNoContent());
        tokenRequest("refresh", field(two, "refreshToken"), 401);
    }

    @Test
    void rejectsBadCredentialsInactiveAccountsAndInactiveOrganizations() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        for (var state : List.of(UserStatus.DISABLED, UserStatus.LOCKED, UserStatus.PENDING)) {
            user.setStatus(state);
            users.saveAndFlush(user);
            login(401);
        }
        user.setStatus(UserStatus.ACTIVE);
        users.saveAndFlush(user);
        var session = login(200);
        jdbc.update("UPDATE organizations SET status='SUSPENDED'");
        login(401);
        tokenRequest("refresh", field(session, "refreshToken"), 401);
    }

    @Test
    void rejectsExpiredRefreshAndInvalidInputs() throws Exception {
        String raw = field(login(200), "refreshToken");
        jdbc.update("UPDATE refresh_tokens SET expires_at=CURRENT_TIMESTAMP-interval '1 second'");
        tokenRequest("refresh", raw, 401);
        tokenRequest("refresh", "unknown", 401);
        tokenRequest("refresh", "", 400);
        tokenRequest("refresh", "a".repeat(257), 400);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void validatesJwtIssuerAudienceTypeExpiryAndSignature() throws Exception {
        var result = login(200);
        var jwt = decoder.decode(field(result, "accessToken"));
        for (var claims : List.of(
                claims(jwt).issuer("wrong").build(), claims(jwt).audience(List.of("wrong")).build(),
                claims(jwt).claim("token_type", "refresh").build(), claims(jwt).subject("invalid").build(),
                claims(jwt).expiresAt(Instant.now().minusSeconds(1)).issuedAt(Instant.now().minusSeconds(600)).build())) {
            me(encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue(), 401);
        }
        String token = field(result, "accessToken");
        int index = token.lastIndexOf('.') + 1;
        me(token.substring(0, index) + (token.charAt(index) == 'a' ? 'b' : 'a') + token.substring(index + 1), 401);
    }

    @Test
    void corsAllowsConfiguredOriginsAndRejectsOthers() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        mvc.perform(post("/api/auth/login").header("Origin", "https://evil.example")
                .contentType(MediaType.APPLICATION_JSON).content(loginBody())).andExpect(status().isForbidden());
    }

    @Test
    void includesCurrentOrganizationRolesAndPermissionsAndRequiresSystemPlatformRole() throws Exception {
        UUID local = UUID.randomUUID();
        UUID foreign = UUID.randomUUID();
        UUID otherOrg = UUID.randomUUID();
        UUID permission = jdbc.queryForObject("SELECT id FROM permissions WHERE code = ?", UUID.class, Permissions.INCIDENT_VIEW);
        UUID foreignPermission = UUID.randomUUID();
        UUID responder = UUID.randomUUID();
        jdbc.update("INSERT INTO organizations(id,name,slug,status,created_at,updated_at) VALUES (?,'Other','other','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", otherOrg);
        jdbc.update("INSERT INTO roles(id,organization_id,name,code,is_system,created_at,updated_at) VALUES (?,?,'Admin','PLATFORM_ADMIN',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", local, user.getOrganization().getId());
        jdbc.update("INSERT INTO roles(id,organization_id,name,code,is_system,created_at,updated_at) VALUES (?,?,'Foreign','FOREIGN',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", foreign, otherOrg);
        jdbc.update("INSERT INTO permissions(id,code) VALUES (?,'foreign:view')", foreignPermission);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", foreign, foreignPermission);
        jdbc.update("INSERT INTO roles(id,organization_id,name,code,is_system,created_at,updated_at) VALUES (?,?,'Responder','RESPONDER',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", responder, user.getOrganization().getId());
        jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", responder, permission);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES (?,?)", user.getId(), responder);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", local, permission);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES (?,?),(?,?)", user.getId(), local, user.getId(), foreign);
        var ordinaryLogin = login(200);
        var ordinary = decoder.decode(field(ordinaryLogin, "accessToken"));
        assertThat(ordinary.getClaimAsStringList("authorities"))
                .containsExactlyInAnyOrder("ROLE_RESPONDER", "incident:view");
        jdbc.update("UPDATE roles SET is_system=true WHERE id=?", local);
        var admin = login(200);
        assertThat(decoder.decode(field(admin, "accessToken")).getClaimAsStringList("authorities"))
                .containsExactlyInAnyOrder("ROLE_PLATFORM_ADMIN", "ROLE_RESPONDER", "incident:view");
        var refreshed = tokenRequest("refresh", field(ordinaryLogin, "refreshToken"), 200);
        assertThat(decoder.decode(field(refreshed, "accessToken")).getClaimAsStringList("authorities"))
                .containsExactlyInAnyOrder("ROLE_PLATFORM_ADMIN", "ROLE_RESPONDER", "incident:view");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + field(admin, "accessToken")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.authorities").isArray());
    }

    private JwtClaimsSet.Builder claims(Jwt jwt) {
        return JwtClaimsSet.builder().claims(c -> c.putAll(jwt.getClaims()));
    }

    private String loginBody() {
        return "{\"email\":\"ALICE@example.com\",\"password\":\"secure-password\"}";
    }

    private MvcResult login(int expected) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody()))
                .andExpect(status().is(expected)).andReturn();
    }

    private MvcResult tokenRequest(String action, String token, Integer expected) throws Exception {
        var request = mvc.perform(post("/api/auth/" + action).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("refreshToken", token))));
        if (expected != null) request.andExpect(status().is(expected));
        return request.andReturn();
    }

    private String field(MvcResult result, String name) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).get(name).asString();
    }

    private void me(String token, int expected) throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().is(expected));
    }
}
