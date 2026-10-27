package com.husseinabonoktah.opsflowbackend.user;

import com.husseinabonoktah.opsflowbackend.organization.domain.Organization;
import com.husseinabonoktah.opsflowbackend.organization.domain.OrganizationStatus;
import com.husseinabonoktah.opsflowbackend.organization.infrastructure.persistence.OrganizationRepository;
import com.husseinabonoktah.opsflowbackend.user.infrastructure.persistence.UserRepository;
import com.husseinabonoktah.opsflowbackend.shared.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class UserIntegrationIT extends BaseIntegrationTest {
    private static final String URL = "/api/users";
    private static final String PASSWORD = "a-secure-password";

    @Autowired UserRepository repository;
    @Autowired OrganizationRepository organizations;
    @Autowired PasswordEncoder passwordEncoder;
    private UUID organizationId;

    @BeforeEach
    void createOrganization() {
        Organization organization = new Organization();
        organization.setName("User Test Organization");
        organization.setSlug("users-" + UUID.randomUUID());
        organization.setStatus(OrganizationStatus.ACTIVE);
        organizationId = organizations.saveAndFlush(organization).getId();
    }

    @Test
    void createsReadsAndUpdatesUserWithoutExposingOrChangingPassword() throws Exception {
        UUID id = create("  Alice@Example.com  ");
        var user = repository.findById(id).orElseThrow();
        String hash = user.getPasswordHash();
        assertThat(hash).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, hash)).isTrue();
        assertThat(passwordEncoder.matches("incorrect-password", hash)).isFalse();
        mvc.perform(get(URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(organizationId.toString()))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("alice@example.com", organizationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.status").value("DISABLED"));
        assertThat(repository.findById(id).orElseThrow().getPasswordHash()).isEqualTo(hash);
    }

    @Test
    void rejectsDuplicateEmailOnCreateAndUpdate() throws Exception {
        create("alice@example.com");
        UUID second = create("second@example.com");
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(" ALICE@EXAMPLE.COM ", organizationId)))
                .andExpect(status().isConflict());
        mvc.perform(put(URL + "/" + second).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("ALICE@example.com", organizationId)))
                .andExpect(status().isConflict());
        assertThat(repository.findById(second).orElseThrow().getEmail()).isEqualTo("second@example.com");
    }

    @Test
    void returnsPaginatedUsers() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        create("first@example.com");
        create("second@example.com");
        mvc.perform(get(URL).param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void returnsNotFoundForMissingUserOrOrganization() throws Exception {
        String missing = URL + "/" + UUID.randomUUID();
        mvc.perform(get(missing)).andExpect(status().isNotFound());
        mvc.perform(put(missing).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("missing@example.com", organizationId)))
                .andExpect(status().isNotFound());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("missing@example.com", UUID.randomUUID())))
                .andExpect(status().isNotFound());
        UUID id = create("existing@example.com");
        mvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("changed@example.com", UUID.randomUUID())))
                .andExpect(status().isNotFound());
        assertThat(repository.findById(id).orElseThrow().getEmail()).isEqualTo("existing@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "{\"email\":\"bad-email\"}"})
    void rejectsInvalidRequests(String body) throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(put(URL + "/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test
    void validatesEmailPasswordAndNameLimits() throws Exception {
        String valid = createBody("alice@example.com", organizationId);
        for (String invalid : new String[]{
                valid.replace("alice@example.com", "invalid-email"),
                valid.replace(PASSWORD, "short"),
                valid.replace(PASSWORD, "p".repeat(129)),
                valid.replace(" Alice ", " "),
                valid.replace(" Alice ", "a".repeat(101))}) {
            mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").exists());
        }
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=abc"})
    void rejectsInvalidPagination(String query) throws Exception {
        mvc.perform(get(URL + "?" + query)).andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void requiresAuthentication() throws Exception {
        mvc.perform(get(URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("alice@example.com", organizationId)))
                .andExpect(status().isUnauthorized());
        mvc.perform(put(URL + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("alice@example.com", organizationId)))
                .andExpect(status().isUnauthorized());
        assertThat(repository.count()).isZero();
    }

    @Test
    void acceptsJwtAuthenticatedWritesWithoutCsrf() throws Exception {
        mvc.perform(post(URL).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("alice@example.com", organizationId)))
                .andExpect(status().isCreated());
        UUID id = repository.findByEmailIgnoreCase("alice@example.com").orElseThrow().getId();
        mvc.perform(put(URL + "/" + id).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("alice@example.com", organizationId)))
                .andExpect(status().isOk());
        assertThat(repository.findById(id).orElseThrow().getFirstName()).isEqualTo("Updated");
    }

    private UUID create(String email) throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(email, organizationId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(URL + "/")))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        return repository.findAll().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email.strip()))
                .findFirst().orElseThrow().getId();
    }

    private String createBody(String email, UUID organization) {
        return """
                {"organizationId":"%s","firstName":" Alice ","lastName":" Smith ",
                 "email":"%s","password":"%s"}
                """.formatted(organization, email, PASSWORD);
    }

    private String updateBody(String email, UUID organization) {
        return """
                {"organizationId":"%s","firstName":"Updated","lastName":"Smith",
                 "email":"%s","status":"DISABLED"}
                """.formatted(organization, email);
    }
}
