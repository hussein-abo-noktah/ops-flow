package com.husseinabonoktah.opsflowbackend.incident;

import com.husseinabonoktah.opsflowbackend.shared.BaseIntegrationTest;
import com.husseinabonoktah.opsflowbackend.incident.domain.*;
import com.husseinabonoktah.opsflowbackend.incident.infrastructure.persistence.IncidentRepository;
import com.husseinabonoktah.opsflowbackend.organization.domain.*;
import com.husseinabonoktah.opsflowbackend.organization.infrastructure.persistence.OrganizationRepository;
import com.husseinabonoktah.opsflowbackend.team.domain.*;
import com.husseinabonoktah.opsflowbackend.team.infrastructure.persistence.TeamRepository;
import com.husseinabonoktah.opsflowbackend.user.domain.*;
import com.husseinabonoktah.opsflowbackend.user.infrastructure.persistence.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class IncidentSearchIntegrationIT extends BaseIntegrationTest {
    private static final String URL = "/api/incidents";
    private static final Instant TIME = Instant.parse("2026-10-01T10:00:00Z");
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired TeamRepository teams;
    @Autowired IncidentRepository incidents;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    private Organization organization;
    private User actor;
    private Team team;

    @BeforeEach
    void setup() {
        organization = organization();
        actor = user(organization);
        team = new Team();
        team.setOrganization(organization);
        team.setName("Operations");
        team.setCode("OPS");
        team.setStatus(TeamStatus.ACTIVE);
        teams.saveAndFlush(team);
    }

    @Test
    void combinesAllFiltersAndNeverIncludesForeignMatches() throws Exception {
        Incident match = incident(organization, actor, "API outage", "Database errors", Severity.SEV1, TIME);
        match.acknowledge();
        match.assign(team, actor);
        incidents.saveAndFlush(match);
        incident(organization, actor, "API outage", null, Severity.SEV1, TIME);
        Organization foreign = organization();
        Incident foreignMatch = incident(foreign, user(foreign), "API outage", "Database errors", Severity.SEV1, TIME);
        foreignMatch.acknowledge();
        incidents.saveAndFlush(foreignMatch);

        mvc.perform(get(URL).with(auth("ROLE_PLATFORM_ADMIN"))
                        .param("q", "  api  ").param("status", "ACKNOWLEDGED").param("severity", "SEV1")
                        .param("assignedTeamId", team.getId().toString()).param("assignedUserId", actor.getId().toString())
                        .param("createdBy", actor.getId().toString())
                        .param("createdFrom", TIME.toString()).param("createdTo", TIME.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(match.getId().toString()));
        mvc.perform(get(URL).with(auth("incident:view")).param("q", "api"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(2));
        for (String field : List.of("assignedTeamId", "assignedUserId", "createdBy")) {
            mvc.perform(get(URL).with(auth("incident:view")).param(field, UUID.randomUUID().toString()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(0));
        }
        mvc.perform(get(URL).with(auth("incident:view")).param("createdBy", foreignMatch.getCreatedBy().getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void statusSeverityAndAssignmentFiltersWorkIndependentlyAndIntersect() throws Exception {
        Incident first = incident(organization, actor, "First", null, Severity.SEV1, TIME);
        first.acknowledge();
        first.assign(team, actor);
        incidents.saveAndFlush(first);
        Incident second = incident(organization, actor, "Second", null, Severity.SEV2, TIME);
        second.acknowledge();
        incidents.saveAndFlush(second);
        incident(organization, actor, "Third", null, Severity.SEV1, TIME);
        for (var filter : Map.of("status", "ACKNOWLEDGED", "severity", "SEV1").entrySet()) {
            mvc.perform(get(URL).with(auth("incident:view")).param(filter.getKey(), filter.getValue()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(2));
        }
        for (var filter : Map.of("assignedTeamId", team.getId().toString(),
                "assignedUserId", actor.getId().toString()).entrySet()) {
            mvc.perform(get(URL).with(auth("incident:view")).param(filter.getKey(), filter.getValue()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(first.getId().toString()));
        }
        mvc.perform(get(URL).with(auth("incident:view")).param("status", "OPEN").param("severity", "SEV2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get(URL).with(auth("incident:view")).param("createdBy", actor.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void searchesTitleDescriptionAndNumberIgnoringCaseAndTreatsWildcardsLiterally() throws Exception {
        Incident title = incident(organization, actor, "API outage", null, Severity.SEV1, TIME);
        Incident description = incident(organization, actor, "Storage", "API failures", Severity.SEV2, TIME);
        Incident special = incident(organization, actor, "100%_\\ complete", null, Severity.SEV3, TIME);
        incident(organization, actor, "100xx complete", null, Severity.SEV4, TIME);
        mvc.perform(get(URL).with(auth("incident:view")).param("q", "aPi").param("sortBy", "title"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(title.getId().toString()))
                .andExpect(jsonPath("$.content[1].id").value(description.getId().toString()));
        mvc.perform(get(URL).with(auth("incident:view")).param("q", title.getIncidentNumber().toLowerCase(Locale.ROOT)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(title.getId().toString()));
        for (String literal : List.of("%", "_", "\\", "100%_\\")) {
            mvc.perform(get(URL).with(auth("incident:view")).param("q", literal))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(special.getId().toString()));
        }
        mvc.perform(get(URL).with(auth("incident:view")).param("q", "   "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(4));
    }

    @Test
    void dateBoundsAreInclusiveAndCanBeUsedSeparately() throws Exception {
        incident(organization, actor, "Before", null, Severity.SEV1, TIME.minusSeconds(1));
        Incident match = incident(organization, actor, "At bound", null, Severity.SEV1, TIME);
        incident(organization, actor, "After", null, Severity.SEV1, TIME.plusSeconds(1));
        mvc.perform(get(URL).with(auth("incident:view")).param("createdFrom", TIME.toString()).param("createdTo", TIME.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(match.getId().toString()));
        for (String bound : List.of("createdFrom", "createdTo")) {
            mvc.perform(get(URL).with(auth("incident:view")).param(bound, TIME.toString()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(2));
        }
        mvc.perform(get(URL).with(auth("incident:view")).param("createdFrom", "2026-10-01T14:00:00+04:00")
                        .param("createdTo", TIME.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void paginatesWithStableTiesAndSupportsAscendingAndDescendingSorts() throws Exception {
        Incident alpha = incident(organization, actor, "Alpha", null, Severity.SEV1, TIME);
        Incident bravo = incident(organization, actor, "Bravo", null, Severity.SEV2, TIME);
        Incident charlie = incident(organization, actor, "Charlie", null, Severity.SEV3, TIME);
        List<String> ids = Stream.of(alpha, bravo, charlie).map(i -> i.getId().toString()).sorted().toList();
        for (int page = 0; page < 3; page++) {
            mvc.perform(get(URL).with(auth("incident:view")).param("size", "1").param("page", String.valueOf(page)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(3))
                    .andExpect(jsonPath("$.page.totalPages").value(3))
                    .andExpect(jsonPath("$.content[0].id").value(ids.get(page)));
        }
        mvc.perform(get(URL).with(auth("incident:view")).param("sortBy", "title").param("direction", "DESC")
                        .param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].title").value("Charlie"))
                .andExpect(jsonPath("$.content[1].title").value("Bravo"));
        mvc.perform(get(URL).with(auth("incident:view")).param("sortBy", "severity").param("direction", "ASC"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].title").value("Alpha"));
        mvc.perform(get(URL).with(auth("incident:view")).param("sortBy", "id").param("direction", "DESC"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(ids.getLast()));
        mvc.perform(get(URL).with(auth("incident:view")).param("page", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(3));
        assertThat(ids).hasSize(3);
    }

    @Test
    void rejectsInvalidFiltersSortFieldsDateRangesAndPagination() throws Exception {
        for (var invalid : List.of(
                Map.entry("status", "BOGUS"), Map.entry("severity", "SEV0"),
                Map.entry("assignedTeamId", "invalid"), Map.entry("assignedUserId", "invalid"),
                Map.entry("createdBy", "invalid"), Map.entry("createdFrom", "not-a-date"),
                Map.entry("createdTo", "2026-10-01"), Map.entry("sortBy", "organization.name"),
                Map.entry("sortBy", "unknown"), Map.entry("sortBy", ""),
                Map.entry("direction", "sideways"), Map.entry("page", "-1"),
                Map.entry("page", "abc"), Map.entry("size", "0"), Map.entry("size", "101"),
                Map.entry("q", "x".repeat(251)))) {
            mvc.perform(get(URL).with(auth("incident:view")).param(invalid.getKey(), invalid.getValue()))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        }
        mvc.perform(get(URL).with(auth("incident:view")).param("createdFrom", TIME.plusSeconds(1).toString())
                        .param("createdTo", TIME.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get(URL).with(auth("incident:view")).param("page", "2147483647").param("size", "100"))
                .andExpect(status().isBadRequest());
    }

    private JwtRequestPostProcessor auth(String authority) {
        return jwt().jwt(j -> j.subject(actor.getId().toString()).claim("org_id", organization.getId().toString()))
                .authorities(new SimpleGrantedAuthority(authority));
    }

    private Organization organization() {
        Organization value = new Organization();
        value.setName("Search test");
        value.setSlug("search-" + UUID.randomUUID());
        value.setStatus(OrganizationStatus.ACTIVE);
        return organizations.saveAndFlush(value);
    }

    private User user(Organization org) {
        User value = new User();
        value.setOrganization(org);
        value.setFirstName("Alice");
        value.setLastName("Smith");
        value.setEmail(UUID.randomUUID() + "@example.com");
        value.setPasswordHash("not-exposed");
        value.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(value);
    }

    private Incident incident(Organization org, User creator, String title, String description,
                              Severity severity, Instant createdAt) {
        Incident value = incidents.saveAndFlush(Incident.open(org, "INC-" + UUID.randomUUID(),
                title, description, severity, creator));
        jdbc.update("UPDATE incidents SET created_at = ? WHERE id = ?", Timestamp.from(createdAt), value.getId());
        entityManager.refresh(value);
        return value;
    }
}
