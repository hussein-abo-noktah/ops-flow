package com.husseinabonoktah.opsflowbackend.team;

import com.husseinabonoktah.opsflowbackend.organization.domain.*;
import com.husseinabonoktah.opsflowbackend.organization.infrastructure.persistence.OrganizationRepository;
import com.husseinabonoktah.opsflowbackend.shared.BaseIntegrationTest;
import com.husseinabonoktah.opsflowbackend.team.domain.*;
import com.husseinabonoktah.opsflowbackend.team.infrastructure.persistence.*;
import com.husseinabonoktah.opsflowbackend.user.domain.*;
import com.husseinabonoktah.opsflowbackend.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class TeamIntegrationIT extends BaseIntegrationTest {
    private static final String URL = "/api/teams";
    @Autowired OrganizationRepository organizations;
    @Autowired TeamRepository teams;
    @Autowired TeamMemberRepository members;
    @Autowired UserRepository users;
    private Organization organization;
    private Organization otherOrganization;

    @BeforeEach
    void setup() {
        organization = organization();
        otherOrganization = organization();
    }

    @Test
    void createsReadsUpdatesAndPaginatesWithinCurrentTenant() throws Exception {
        mvc.perform(post(URL).with(tenant("team:create")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" Operations ","code":" ops ","description":" Support "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.organizationId").value(organization.getId().toString()))
                .andExpect(jsonPath("$.name").value("Operations"))
                .andExpect(jsonPath("$.code").value("OPS"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
        Team team = teams.findAllByOrganizationId(organization.getId(),
                org.springframework.data.domain.Pageable.unpaged()).getContent().getFirst();
        team(otherOrganization, "OTHER");
        mvc.perform(get(URL + "/" + team.getId()).with(tenant()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OPS"));
        mvc.perform(get(URL).with(tenant()).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(team.getId().toString()));
        mvc.perform(put(URL + "/" + team.getId()).with(tenant("team:update"))
                        .contentType(MediaType.APPLICATION_JSON).content(update("new", "DISABLED")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("NEW"))
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.organizationId").value(organization.getId().toString()));
    }

    @Test
    void enforcesTenantScopedCodeUniquenessOnCreateAndUpdate() throws Exception {
        team(organization, "OPS");
        mvc.perform(post(URL).with(tenant("team:create")).contentType(MediaType.APPLICATION_JSON)
                        .content(""" 
                                {"name":"Ops","code":" ops "}
                                """))
                .andExpect(status().isConflict());
        Team second = team(organization, "SECOND");
        mvc.perform(put(URL + "/" + second.getId()).with(tenant("team:update"))
                        .contentType(MediaType.APPLICATION_JSON).content(update("ops", "ACTIVE")))
                .andExpect(status().isConflict());
        mvc.perform(post(URL).with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())
                                .claim("org_id", otherOrganization.getId().toString()))
                        .authorities(new SimpleGrantedAuthority("team:create")))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Ops","code":"ops"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void hidesOtherTenantsTeamsAcrossEveryEndpointEvenFromPlatformAdmin() throws Exception {
        Team team = team(otherOrganization, "PRIVATE");
        String path = URL + "/" + team.getId();
        var auth = tenant("ROLE_PLATFORM_ADMIN");
        mvc.perform(get(path).with(auth)).andExpect(status().isNotFound());
        mvc.perform(put(path).with(auth).contentType(MediaType.APPLICATION_JSON)
                .content(update("CHANGED", "ACTIVE"))).andExpect(status().isNotFound());
        mvc.perform(get(path + "/members").with(auth)).andExpect(status().isNotFound());
        mvc.perform(post(path + "/members").with(auth).contentType(MediaType.APPLICATION_JSON)
                .content(add(UUID.randomUUID(), false))).andExpect(status().isNotFound());
        mvc.perform(delete(path + "/members/" + UUID.randomUUID()).with(auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void addsListsAndRemovesMembershipWithoutExposingPassword() throws Exception {
        Team team = team(organization, "OPS");
        User user = user(organization, UserStatus.ACTIVE);
        String path = URL + "/" + team.getId() + "/members";
        mvc.perform(post(path).with(tenant("team:update")).contentType(MediaType.APPLICATION_JSON)
                        .content(add(user.getId(), true)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", path + "/" + user.getId()))
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.primary").value(true))
                .andExpect(jsonPath("$.joinedAt").isNotEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get(path).with(tenant()).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value(user.getEmail()));
        mvc.perform(delete(path + "/" + user.getId()).with(tenant("team:update")))
                .andExpect(status().isNoContent());
        assertThat(members.existsByTeamIdAndUserId(team.getId(), user.getId())).isFalse();
        mvc.perform(delete(path + "/" + user.getId()).with(tenant("team:update")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsCrossTenantAndMissingUsers() throws Exception {
        Team team = team(organization, "OPS");
        User foreign = user(otherOrganization, UserStatus.ACTIVE);
        for (UUID id : new UUID[]{foreign.getId(), UUID.randomUUID()}) {
            mvc.perform(post(URL + "/" + team.getId() + "/members").with(tenant("team:update"))
                            .contentType(MediaType.APPLICATION_JSON).content(add(id, false)))
                    .andExpect(status().isNotFound());
        }
        assertThat(members.count()).isZero();
    }

    @Test
    void preventsDuplicateMembershipAndMultiplePrimaryTeamsButAllowsSecondaryTeams() throws Exception {
        Team first = team(organization, "FIRST");
        Team second = team(organization, "SECOND");
        User user = user(organization, UserStatus.ACTIVE);
        mvc.perform(post(URL + "/" + first.getId() + "/members").with(tenant("team:update"))
                .contentType(MediaType.APPLICATION_JSON).content(add(user.getId(), true)))
                .andExpect(status().isCreated());
        mvc.perform(post(URL + "/" + first.getId() + "/members").with(tenant("team:update"))
                .contentType(MediaType.APPLICATION_JSON).content(add(user.getId(), false)))
                .andExpect(status().isConflict());
        mvc.perform(post(URL + "/" + second.getId() + "/members").with(tenant("team:update"))
                .contentType(MediaType.APPLICATION_JSON).content(add(user.getId(), true)))
                .andExpect(status().isConflict());
        mvc.perform(post(URL + "/" + second.getId() + "/members").with(tenant("team:update"))
                .contentType(MediaType.APPLICATION_JSON).content(add(user.getId(), false)))
                .andExpect(status().isCreated());
        assertThat(members.count()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DISABLED", "LOCKED", "PENDING"})
    void rejectsInactiveUsers(String value) throws Exception {
        Team team = team(organization, "OPS");
        User user = user(organization, UserStatus.valueOf(value));
        mvc.perform(post(URL + "/" + team.getId() + "/members").with(tenant("team:update"))
                        .contentType(MediaType.APPLICATION_JSON).content(add(user.getId(), false)))
                .andExpect(status().isConflict());
        assertThat(members.count()).isZero();
    }

    @Test
    void rejectsAddingToDisabledTeam() throws Exception {
        Team team = team(organization, "OPS");
        team.setStatus(TeamStatus.DISABLED);
        teams.saveAndFlush(team);
        User user = user(organization, UserStatus.ACTIVE);
        mvc.perform(post(URL + "/" + team.getId() + "/members").with(tenant("team:update"))
                        .contentType(MediaType.APPLICATION_JSON).content(add(user.getId(), false)))
                .andExpect(status().isConflict());
    }

    @Test
    void requiresWritePermissionsAndAuthentication() throws Exception {
        Team team = team(organization, "OPS");
        String path = URL + "/" + team.getId();
        mvc.perform(post(URL).with(tenant("PERM_team:create")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Ops","code":"PREFIXED"}
                        """)).andExpect(status().isForbidden());
        mvc.perform(delete(path + "/members/" + UUID.randomUUID()).with(tenant("PERM_team:update")))
                .andExpect(status().isForbidden());
        mvc.perform(post(URL).with(tenant()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Ops","code":"NEW"}
                        """))
                .andExpect(status().isForbidden());
        mvc.perform(put(path).with(tenant("team:create")).contentType(MediaType.APPLICATION_JSON)
                .content(update("CHANGED", "ACTIVE"))).andExpect(status().isForbidden());
        mvc.perform(post(path + "/members").with(tenant()).contentType(MediaType.APPLICATION_JSON)
                .content(add(UUID.randomUUID(), false))).andExpect(status().isForbidden());
        mvc.perform(delete(path + "/members/" + UUID.randomUUID()).with(tenant()))
                .andExpect(status().isForbidden());
        mvc.perform(get(URL).with(anonymous())).andExpect(status().isUnauthorized());
        mvc.perform(post(URL).with(anonymous()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Ops","code":"NEW"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validatesBodiesAndPagination() throws Exception {
        Team team = team(organization, "OPS");
        for (String body : new String[]{"{}", "{", "{\"name\":\" \",\"code\":\" \"}",
                "{\"name\":\"" + "a".repeat(151) + "\",\"code\":\"OPS\"}",
                "{\"name\":\"Ops\",\"code\":\"" + "a".repeat(51) + "\"}",
                "{\"name\":\"Ops\",\"code\":\"OPS\",\"description\":\"" + "a".repeat(1001) + "\"}"}) {
            mvc.perform(post(URL).with(tenant("team:create")).contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(put(URL + "/" + team.getId()).with(tenant("team:update"))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Ops","code":"OPS"}
                        """)).andExpect(status().isBadRequest());
        mvc.perform(post(URL + "/" + team.getId() + "/members").with(tenant("team:update"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        for (String query : new String[]{"page=-1", "size=0", "size=101", "page=abc"}) {
            mvc.perform(get(URL + "?" + query).with(tenant())).andExpect(status().isBadRequest());
            mvc.perform(get(URL + "/" + team.getId() + "/members?" + query).with(tenant()))
                    .andExpect(status().isBadRequest());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void serializesConcurrentDuplicateAndPrimaryMembershipRequests(boolean differentTeams) throws Exception {
        Team first = team(organization, "FIRST");
        Team second = team(organization, "SECOND");
        User user = user(organization, UserStatus.ACTIVE);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var start = new java.util.concurrent.CountDownLatch(1);
            var one = executor.submit(() -> {
                start.await();
                return mvc.perform(post(URL + "/" + first.getId() + "/members")
                        .with(tenant("team:update")).contentType(MediaType.APPLICATION_JSON)
                        .content(add(user.getId(), true))).andReturn().getResponse().getStatus();
            });
            var two = executor.submit(() -> {
                start.await();
                return mvc.perform(post(URL + "/" + (differentTeams ? second : first).getId() + "/members")
                        .with(tenant("team:update")).contentType(MediaType.APPLICATION_JSON)
                        .content(add(user.getId(), true))).andReturn().getResponse().getStatus();
            });
            start.countDown();
            assertThat(java.util.List.of(one.get(20, java.util.concurrent.TimeUnit.SECONDS),
                    two.get(20, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
            assertThat(members.findAll().stream().filter(m -> m.getUser().getId().equals(user.getId())))
                    .hasSize(1);
        } finally {
            members.deleteAll(members.findAllByTeamId(first.getId(),
                    org.springframework.data.domain.Pageable.unpaged()).getContent());
            members.deleteAll(members.findAllByTeamId(second.getId(),
                    org.springframework.data.domain.Pageable.unpaged()).getContent());
            teams.deleteById(first.getId());
            teams.deleteById(second.getId());
            users.deleteById(user.getId());
            organizations.deleteById(organization.getId());
            organizations.deleteById(otherOrganization.getId());
        }
    }

    private JwtRequestPostProcessor tenant(String... authorities) {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString())
                .claim("org_id", organization.getId().toString()))
                .authorities(java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new)
                        .toArray(SimpleGrantedAuthority[]::new));
    }

    private Organization organization() {
        Organization value = new Organization();
        value.setName("Team test");
        value.setSlug("teams-" + UUID.randomUUID());
        value.setStatus(OrganizationStatus.ACTIVE);
        return organizations.saveAndFlush(value);
    }

    private Team team(Organization org, String code) {
        Team value = new Team();
        value.setOrganization(org);
        value.setName("Operations");
        value.setCode(code);
        value.setStatus(TeamStatus.ACTIVE);
        return teams.saveAndFlush(value);
    }

    private User user(Organization org, UserStatus status) {
        User value = new User();
        value.setOrganization(org);
        value.setFirstName("Alice");
        value.setLastName("Smith");
        value.setEmail(UUID.randomUUID() + "@example.com");
        value.setPasswordHash("not-exposed");
        value.setStatus(status);
        return users.saveAndFlush(value);
    }

    private String update(String code, String status) {
        return """
                {"name":"Updated","code":"%s","status":"%s"}
                """.formatted(code, status);
    }

    private String add(UUID userId, boolean primary) {
        return """
                {"userId":"%s","primary":%s}
                """.formatted(userId, primary);
    }
}
