package com.husseinabonoktah.opsflowbackend.incident;

import com.husseinabonoktah.opsflowbackend.shared.BaseIntegrationTest;
import com.husseinabonoktah.opsflowbackend.incident.domain.*;
import com.husseinabonoktah.opsflowbackend.incident.infrastructure.persistence.*;
import com.husseinabonoktah.opsflowbackend.organization.domain.*;
import com.husseinabonoktah.opsflowbackend.organization.infrastructure.persistence.OrganizationRepository;
import com.husseinabonoktah.opsflowbackend.user.domain.*;
import com.husseinabonoktah.opsflowbackend.user.infrastructure.persistence.UserRepository;
import com.husseinabonoktah.opsflowbackend.team.domain.*;
import com.husseinabonoktah.opsflowbackend.team.infrastructure.persistence.TeamRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.data.domain.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class IncidentIntegrationIT extends BaseIntegrationTest {
    private static final String URL = "/api/incidents";
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired TeamRepository teams;
    @Autowired IncidentRepository incidents;
    @Autowired IncidentTimelineRepository timeline;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    private Organization organization;
    private User actor;

    @BeforeEach
    void setup() {
        organization = organization();
        actor = user(organization);
    }

    @Test
    void createsAndCompletesWorkflowWithActorTimelineAndTimestamps() throws Exception {
        mvc.perform(post(URL).with(auth("incident:create")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":" Outage ","severity":"SEV1"}
                        """))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("Outage"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdBy").value(actor.getId().toString()));
        Incident incident = incidents.findAllByOrganizationId(organization.getId(), Pageable.unpaged()).getContent().getFirst();
        String path = URL + "/" + incident.getId();
        String[] steps = {"acknowledge", "start-investigation", "mitigate", "resolve", "close"};
        for (int i = 0; i < steps.length; i++) {
            mvc.perform(post(path + "/" + steps[i]).with(auth(i < 3 ? "incident:update" : "incident:resolve")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(IncidentStatus.values()[i + 1].name()));
        }
        mvc.perform(get(path).with(auth("incident:view"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledgedAt").isNotEmpty())
                .andExpect(jsonPath("$.mitigatedAt").isNotEmpty())
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty())
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
        mvc.perform(get(path + "/timeline").with(auth("incident:view")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(6))
                .andExpect(jsonPath("$.content[0].eventType").value("INCIDENT_CREATED"))
                .andExpect(jsonPath("$.content[5].oldValue").value("RESOLVED"))
                .andExpect(jsonPath("$.content[5].newValue").value("CLOSED"))
                .andExpect(jsonPath("$.content[5].actorId").value(actor.getId().toString()));
        mvc.perform(put(path).with(auth("incident:update")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Changed"}
                        """)).andExpect(status().isConflict());
    }

    @Test
    void rejectsSkippedAndRepeatedTransitionsWithoutTimelineEvents() throws Exception {
        Incident incident = incident(organization, actor);
        String path = URL + "/" + incident.getId();
        mvc.perform(post(path + "/resolve").with(auth("incident:resolve"))).andExpect(status().isConflict());
        assertThat(events(incident)).isEmpty();
        mvc.perform(post(path + "/acknowledge").with(auth("incident:update"))).andExpect(status().isOk());
        mvc.perform(post(path + "/acknowledge").with(auth("incident:update"))).andExpect(status().isConflict());
        assertThat(events(incident)).hasSize(1);
    }

    @Test
    void updatesDetailsAndSeverityWithoutAllowingStatusChangesOrNoOpEvents() throws Exception {
        Incident incident = incident(organization, actor);
        String path = URL + "/" + incident.getId();
        mvc.perform(put(path).with(auth("incident:update")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Updated","description":"Details","status":"CLOSED"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OPEN"));
        for (int i = 0; i < 2; i++) {
            mvc.perform(post(path + "/severity").with(auth("incident:update")).contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"severity":"SEV1"}
                            """)).andExpect(status().isOk()).andExpect(jsonPath("$.severity").value("SEV1"));
        }
        assertThat(events(incident)).extracting(IncidentTimeline::getEventType)
                .containsExactly(TimelineEventType.INCIDENT_UPDATED, TimelineEventType.SEVERITY_CHANGED);
    }

    @Test
    void assignmentValidatesTenantAndActivityAndSupportsUnassigning() throws Exception {
        Incident incident = incident(organization, actor);
        Team team = new Team();
        team.setOrganization(organization); team.setName("Ops"); team.setCode("OPS"); team.setStatus(TeamStatus.ACTIVE);
        teams.saveAndFlush(team);
        String path = URL + "/" + incident.getId() + "/assign";
        String body = """
                {"teamId":"%s","userId":"%s"}
                """.formatted(team.getId(), actor.getId());
        mvc.perform(post(path).with(auth("incident:assign")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assignedTeamId").value(team.getId().toString()));
        mvc.perform(post(path).with(auth("incident:assign")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assignedUserId").isEmpty());
        User foreign = user(organization());
        mvc.perform(post(path).with(auth("incident:assign")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId":"%s"}
                        """.formatted(foreign.getId()))).andExpect(status().isNotFound());
        team.setStatus(TeamStatus.DISABLED); teams.saveAndFlush(team);
        mvc.perform(post(path).with(auth("incident:assign")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        team.setStatus(TeamStatus.ACTIVE); teams.saveAndFlush(team);
        actor.setStatus(UserStatus.DISABLED); users.saveAndFlush(actor);
        mvc.perform(post(path).with(auth("incident:assign")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        assertThat(events(incident)).hasSize(2);
    }

    @Test
    void hidesForeignIncidentsAcrossEveryEndpointEvenFromPlatformAdmin() throws Exception {
        Organization foreign = organization();
        Incident incident = incident(foreign, user(foreign));
        String path = URL + "/" + incident.getId();
        mvc.perform(get(path).with(auth("ROLE_PLATFORM_ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(get(path + "/timeline").with(auth("ROLE_PLATFORM_ADMIN"))).andExpect(status().isNotFound());
        for (String action : List.of("acknowledge", "start-investigation", "mitigate", "resolve", "close"))
            mvc.perform(post(path + "/" + action).with(auth("ROLE_PLATFORM_ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(put(path).with(auth("ROLE_PLATFORM_ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Changed"}
                        """)).andExpect(status().isNotFound());
        mvc.perform(post(path + "/assign").with(auth("ROLE_PLATFORM_ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isNotFound());
        mvc.perform(post(path + "/severity").with(auth("ROLE_PLATFORM_ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"severity":"SEV1"}
                        """)).andExpect(status().isNotFound());
        mvc.perform(get(URL).with(auth("incident:view"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void validatesBodiesPaginationAndPermissions() throws Exception {
        Incident incident = incident(organization, actor);
        String path = URL + "/" + incident.getId();
        mvc.perform(get(URL).with(anonymous())).andExpect(status().isUnauthorized());
        mvc.perform(get(URL).with(auth())).andExpect(status().isForbidden());
        mvc.perform(get(path + "/timeline").with(auth())).andExpect(status().isForbidden());
        mvc.perform(post(URL).with(auth("incident:update")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Outage","severity":"SEV1"}
                        """)).andExpect(status().isForbidden());
        for (String action : List.of("acknowledge", "start-investigation", "mitigate", "resolve", "close"))
            mvc.perform(post(path + "/" + action).with(auth())).andExpect(status().isForbidden());
        mvc.perform(post(path + "/resolve").with(auth("incident:update"))).andExpect(status().isForbidden());
        mvc.perform(post(path + "/assign").with(auth("incident:update")).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
        mvc.perform(put(path).with(auth("incident:assign")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Changed"}
                        """)).andExpect(status().isForbidden());
        mvc.perform(post(path + "/severity").with(auth("incident:assign")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"severity":"SEV1"}
                        """)).andExpect(status().isForbidden());
        for (String body : List.of("{}", "{", """
                {"title":" ","severity":"SEV1"}
                """, """
                {"title":"Outage","severity":"INVALID"}
                """, """
                {"title":"%s","severity":"SEV1"}
                """.formatted("a".repeat(251)))) {
            mvc.perform(post(URL).with(auth("incident:create")).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        for (String query : List.of("page=-1", "size=0", "size=101", "page=bad")) {
            mvc.perform(get(URL + "?" + query).with(auth("incident:view"))).andExpect(status().isBadRequest());
            mvc.perform(get(path + "/timeline?" + query).with(auth("incident:view"))).andExpect(status().isBadRequest());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentAcknowledgementsProduceOneTransitionAndOneEvent() throws Exception {
        Incident incident = incident(organization, actor);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Integer> call = () -> {
                start.await();
                return mvc.perform(post(URL + "/" + incident.getId() + "/acknowledge").with(auth("incident:update")))
                        .andReturn().getResponse().getStatus();
            };
            var first = executor.submit(call);
            var second = executor.submit(call);
            start.countDown();
            assertThat(List.of(first.get(20, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(20, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 409);
            assertThat(events(incident)).hasSize(1);
            assertThat(incidents.findById(incident.getId()).orElseThrow().getStatus()).isEqualTo(IncidentStatus.ACKNOWLEDGED);
        } finally {
            com.husseinabonoktah.opsflowbackend.shared.IncidentFixtureCleanup.purge(jdbc, transactionManager, incident.getId());
            users.deleteById(actor.getId()); organizations.deleteById(organization.getId());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollbackRevertsBothIncidentAndTimeline() {
        Incident incident = incident(organization, actor);
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                try {
                    mvc.perform(post(URL + "/" + incident.getId() + "/acknowledge").with(auth("incident:update")))
                            .andExpect(status().isOk());
                } catch (Exception e) { throw new RuntimeException(e); }
                tx.setRollbackOnly();
            });
            assertThat(incidents.findById(incident.getId()).orElseThrow().getStatus()).isEqualTo(IncidentStatus.OPEN);
            assertThat(events(incident)).isEmpty();
        } finally {
            incidents.deleteById(incident.getId()); users.deleteById(actor.getId()); organizations.deleteById(organization.getId());
        }
    }

    private List<IncidentTimeline> events(Incident incident) {
        return timeline.findAllByIncidentIdAndIncidentOrganizationId(incident.getId(),
                organization.getId(), PageRequest.of(0, 100, Sort.by("createdAt", "id"))).getContent();
    }
    private JwtRequestPostProcessor auth(String... permissions) {
        return jwt().jwt(j -> j.subject(actor.getId().toString()).claim("org_id", organization.getId().toString()))
                .authorities(Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(SimpleGrantedAuthority[]::new));
    }
    private Organization organization() {
        Organization value = new Organization(); value.setName("Incidents");
        value.setSlug("incidents-" + UUID.randomUUID()); value.setStatus(OrganizationStatus.ACTIVE);
        return organizations.saveAndFlush(value);
    }
    private User user(Organization org) {
        User value = new User(); value.setOrganization(org); value.setFirstName("Alice"); value.setLastName("Smith");
        value.setEmail(UUID.randomUUID() + "@example.com"); value.setPasswordHash("not-exposed"); value.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(value);
    }
    private Incident incident(Organization org, User creator) {
        return incidents.saveAndFlush(Incident.open(org, "INC-" + UUID.randomUUID(), "Outage", null, Severity.SEV2, creator));
    }
}
