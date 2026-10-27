package com.husseinabonoktah.opsflowbackend.organization;

import com.husseinabonoktah.opsflowbackend.organization.infrastructure.persistence.OrganizationRepository;
import com.husseinabonoktah.opsflowbackend.shared.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrganizationIntegrationIT extends BaseIntegrationTest {
    private static final String URL = "/api/organizations";

    @Autowired OrganizationRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void createsReadsAndUpdatesPersistedOrganization() throws Exception {
        mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  Ops Flow  \",\"slug\":\"  ops-flow  \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(URL + "/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Ops Flow"))
                .andExpect(jsonPath("$.slug").value("ops-flow"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        var saved = repository.findAll().getFirst();
        var createdAt = saved.getCreatedAt();
        mvc.perform(get(URL + "/" + saved.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.slug").value("ops-flow"));

        mvc.perform(put(URL + "/" + saved.getId()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\",\"slug\":\"new-slug\",\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.slug").value("new-slug"))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        var updated = repository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfter(createdAt);
    }

    @Test
    void returnsPaginatedOrganizationsAndEmptyPage() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        create("first");
        create("second");
        mvc.perform(get(URL).param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("second"))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void rejectsDuplicateSlugOnCreateAndUpdateButAllowsOwnSlug() throws Exception {
        UUID first = create("first");
        UUID second = create("second");
        mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Duplicate\",\"slug\":\" first \"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        mvc.perform(put(URL + "/" + second).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Duplicate\",\"slug\":\"first\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isConflict());
        assertThat(repository.findById(second).orElseThrow().getSlug()).isEqualTo("second");
        mvc.perform(put(URL + "/" + first).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\",\"slug\":\"first\",\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISABLED"));
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void returnsNotFoundForMissingOrganizations() throws Exception {
        String path = URL + "/" + UUID.randomUUID();
        mvc.perform(get(path)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(put(path).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Missing\",\"slug\":\"missing\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"name\":\"  \",\"slug\":\"valid\"}",
            "{\"name\":\"Valid\",\"slug\":\"Invalid Slug\"}",
            "{\"name\":\"Valid\",\"slug\":\"double--hyphen\"}",
            "{\"name\":\"Valid\",\"slug\":\"-leading\"}",
            "{\"name\":\"Valid\",\"slug\":null}"
    })
    void rejectsInvalidCreateRequests(String body) throws Exception {
        mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").exists());
        assertThat(repository.count()).isZero();
    }

    @Test
    void enforcesFieldLengthLimits() throws Exception {
        mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "n".repeat(151) + "\",\"slug\":\"" + "s".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.slug").exists());
        mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "n".repeat(150) + "\",\"slug\":\"" + "s".repeat(100) + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void validatesUpdateAndDoesNotChangeStoredData() throws Exception {
        UUID id = create("original");
        mvc.perform(put(URL + "/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"slug\":\"INVALID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.slug").exists())
                .andExpect(jsonPath("$.errors.status").exists());
        assertThat(repository.findById(id).orElseThrow().getSlug()).isEqualTo("original");
    }

    @Test
    void rejectsMalformedJsonInvalidEnumAndUuid() throws Exception {
        mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(URL + "/" + UUID.randomUUID()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Valid\",\"slug\":\"valid\",\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(URL + "/invalid-uuid")).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=abc"})
    void rejectsInvalidPagination(String query) throws Exception {
        mvc.perform(get(URL + "?" + query)).andExpect(status().isBadRequest());
    }

    @Test
    @org.springframework.security.test.context.support.WithAnonymousUser
    void requiresAuthentication() throws Exception {
        mvc.perform(get(URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
    }

    @Test
    void requiresCsrfForWrites() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Valid\",\"slug\":\"valid\"}"))
                .andExpect(status().isForbidden());
    }

    private UUID create(String slug) throws Exception {
        mvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Organization\",\"slug\":\"" + slug + "\"}"))
                .andExpect(status().isCreated());
        return repository.findAll().stream().filter(org -> org.getSlug().equals(slug))
                .findFirst().orElseThrow().getId();
    }
}
