package com.husseinabonoktah.opsflowbackend.incident;

import com.husseinabonoktah.opsflowbackend.shared.*;
import com.husseinabonoktah.opsflowbackend.incident.domain.*;
import com.husseinabonoktah.opsflowbackend.incident.infrastructure.persistence.*;
import com.husseinabonoktah.opsflowbackend.organization.domain.*;
import com.husseinabonoktah.opsflowbackend.organization.infrastructure.persistence.OrganizationRepository;
import com.husseinabonoktah.opsflowbackend.user.domain.*;
import com.husseinabonoktah.opsflowbackend.user.infrastructure.persistence.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class IncidentCollaborationIntegrationIT extends BaseIntegrationTest {
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired IncidentRepository incidents;
    @Autowired IncidentCommentRepository comments;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired ObjectMapper json;
    @Autowired PlatformTransactionManager transactions;
    private Organization organization;
    private User author;
    private Incident incident;
    private String path;

    @BeforeEach
    void setup() {
        organization = organization();
        author = user(organization);
        incident = incidents.saveAndFlush(Incident.open(organization, "INC-" + UUID.randomUUID(),
                "Outage", null, Severity.SEV1, author));
        path = "/api/incidents/" + incident.getId();
    }

    @Test
    void createsEditsAndPreservesOriginalAndEveryRevision() throws Exception {
        UUID id = add(" Original ");
        mvc.perform(get(path + "/comments/" + id).with(auth(author, "incident:view")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.comment").value("Original"))
                .andExpect(jsonPath("$.edited").value(false)).andExpect(jsonPath("$.version").value(0));
        edit(id, "Updated", 0, author, "incident:update", 200);
        edit(id, "Final", 1, author, "incident:update", 200);
        forceChecks();
        mvc.perform(get(path + "/comments/" + id + "/history").with(auth(author, "incident:view")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].content").value("Original"))
                .andExpect(jsonPath("$.content[0].revisionType").value("CREATED"))
                .andExpect(jsonPath("$.content[1].content").value("Updated"))
                .andExpect(jsonPath("$.content[2].content").value("Final"))
                .andExpect(jsonPath("$.content[2].revisionNumber").value(2))
                .andExpect(jsonPath("$.content[2].editorId").value(author.getId().toString()));
        mvc.perform(get(path + "/comments/" + id).with(auth(author, "incident:view")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.comment").value("Final"))
                .andExpect(jsonPath("$.edited").value(true)).andExpect(jsonPath("$.version").value(2));
        mvc.perform(get(path + "/timeline").with(auth(author, "incident:view")).param("eventType", "COMMENT_EDITED")
                        .param("commentId", id.toString()).param("actorId", author.getId().toString()).param("direction", "DESC"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].oldValue").value("Updated"))
                .andExpect(jsonPath("$.content[0].newValue").value("Final"))
                .andExpect(jsonPath("$.content[0].commentId").value(id.toString()));
        mvc.perform(get(path + "/comments").with(auth(author, "incident:view")).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1));
        mvc.perform(get(path + "/comments/" + id + "/history").with(auth(author, "incident:view"))
                        .param("page", "1").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].content").value("Updated"));
    }

    @Test
    void onlyAuthorCanEditEvenWhenAnotherUserIsPlatformAdminAndNoOpsDoNotCreateHistory() throws Exception {
        UUID id = add("Original");
        User other = user(organization);
        edit(id, "Changed", 0, other, "ROLE_PLATFORM_ADMIN", 403);
        edit(id, "Original", 0, author, "incident:update", 200);
        forceChecks();
        mvc.perform(get(path + "/comments/" + id + "/history").with(auth(author, "incident:view")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1));
        edit(id, "Changed", 0, author, "incident:update", 200);
        edit(id, "Stale", 0, author, "incident:update", 409);
        mvc.perform(get(path + "/comments/" + id).with(auth(author, "incident:view")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.comment").value("Changed"));
        mvc.perform(delete(path + "/comments/" + id).with(auth(author, "ROLE_PLATFORM_ADMIN")))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void hidesForeignAndMismatchedIncidentCommentsAndHistory() throws Exception {
        UUID id = add("Private");
        User foreign = user(organization());
        for (String suffix : List.of("/comments", "/comments/" + id, "/comments/" + id + "/history", "/timeline")) {
            mvc.perform(get(path + suffix).with(auth(foreign, "ROLE_PLATFORM_ADMIN")))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(post(path + "/comments").with(auth(foreign, "ROLE_PLATFORM_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(body("Other")))
                .andExpect(status().isNotFound());
        edit(id, "Other", 0, foreign, "ROLE_PLATFORM_ADMIN", 404);
        String mismatch = "/api/incidents/" + UUID.randomUUID() + "/comments/" + id;
        mvc.perform(get(mismatch).with(auth(author, "incident:view"))).andExpect(status().isNotFound());
        mvc.perform(get(mismatch + "/history").with(auth(author, "incident:view"))).andExpect(status().isNotFound());
        mvc.perform(put(mismatch).with(auth(author, "incident:update")).contentType(MediaType.APPLICATION_JSON)
                .content(editBody("Other", 0))).andExpect(status().isNotFound());
    }

    @Test
    void validatesBodiesPermissionsAndInactiveAuthors() throws Exception {
        UUID id = add("Original");
        mvc.perform(get(path + "/comments").with(anonymous())).andExpect(status().isUnauthorized());
        mvc.perform(get(path + "/comments").with(auth(author, "incident:update"))).andExpect(status().isForbidden());
        mvc.perform(post(path + "/comments").with(auth(author, "incident:view"))
                .contentType(MediaType.APPLICATION_JSON).content(body("Other"))).andExpect(status().isForbidden());
        edit(id, "Other", 0, author, "incident:view", 403);
        for (String content : List.of("{}", "{", body(" "), body("a".repeat(10001)))) {
            mvc.perform(post(path + "/comments").with(auth(author, "incident:update"))
                    .contentType(MediaType.APPLICATION_JSON).content(content)).andExpect(status().isBadRequest());
        }
        for (String content : List.of(body("Missing version"), editBody(" ", 0), editBody("Other", -1))) {
            mvc.perform(put(path + "/comments/" + id).with(auth(author, "incident:update"))
                    .contentType(MediaType.APPLICATION_JSON).content(content)).andExpect(status().isBadRequest());
        }
        author.setStatus(UserStatus.DISABLED);
        users.saveAndFlush(author);
        mvc.perform(post(path + "/comments").with(auth(author, "incident:update"))
                .contentType(MediaType.APPLICATION_JSON).content(body("Other"))).andExpect(status().isForbidden());
        edit(id, "Other", 0, author, "incident:update", 403);
    }

    @Test
    void closedIncidentsStillPermitCollaborationAndTimelineDateFiltering() throws Exception {
        incident.acknowledge(); incident.startInvestigation(); incident.mitigate(); incident.resolve(); incident.close();
        incidents.saveAndFlush(incident);
        UUID id = add("Post incident review");
        edit(id, "Review correction", 0, author, "incident:update", 200);
        forceChecks();
        Instant at = jdbc.queryForObject("SELECT created_at FROM incident_timeline WHERE comment_id = ? AND event_type = 'COMMENT_ADDED'",
                (rs, _) -> rs.getTimestamp(1).toInstant(), id);
        mvc.perform(get(path + "/timeline").with(auth(author, "incident:view"))
                        .param("from", at.toString()).param("to", at.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].eventType").value("COMMENT_ADDED"));
        for (var filter : Map.of("actorId", UUID.randomUUID().toString(), "commentId", UUID.randomUUID().toString(),
                "eventType", "INCIDENT_CREATED", "from", at.plusSeconds(100).toString(),
                "to", at.minusSeconds(100).toString()).entrySet()) {
            mvc.perform(get(path + "/timeline").with(auth(author, "incident:view")).param(filter.getKey(), filter.getValue()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(0));
        }
    }

    @Test
    void validatesHistoryPaginationAndTimelineQueries() throws Exception {
        UUID id = add("Original");
        for (String suffix : List.of("/comments", "/comments/" + id + "/history", "/timeline")) {
            for (var invalid : List.of(Map.entry("page", "-1"), Map.entry("size", "0"),
                    Map.entry("size", "101"), Map.entry("page", "abc"))) {
                mvc.perform(get(path + suffix).with(auth(author, "incident:view")).param(invalid.getKey(), invalid.getValue()))
                        .andExpect(status().isBadRequest());
            }
        }
        for (var invalid : List.of(Map.entry("eventType", "BOGUS"), Map.entry("actorId", "bad"),
                Map.entry("commentId", "bad"), Map.entry("from", "bad"), Map.entry("to", "bad"),
                Map.entry("direction", "bad"))) {
            mvc.perform(get(path + "/timeline").with(auth(author, "incident:view")).param(invalid.getKey(), invalid.getValue()))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get(path + "/timeline").with(auth(author, "incident:view"))
                        .param("from", "2026-10-02T00:00:00Z").param("to", "2026-10-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void databaseRejectsHistoryRewritesDeletesTruncationAndUnloggedCommentEdits() throws Exception {
        UUID id = add("Original");
        forceChecks();
        for (String table : List.of("incident_timeline", "incident_comment_revisions")) {
            String column = table.equals("incident_timeline") ? "new_value" : "content";
            rejectSql("UPDATE " + table + " SET " + column + " = 'tampered' WHERE comment_id = '" + id + "'");
            rejectSql("DELETE FROM " + table + " WHERE comment_id = '" + id + "'");
            rejectSql("TRUNCATE " + table);
        }
        rejectSql("UPDATE incident_comments SET user_id = '" + user(organization).getId() + "' WHERE id = '" + id + "'");
        rejectSql("UPDATE incident_comments SET comment = 'unlogged', version = version + 1, is_edited = true WHERE id = '" + id + "'");
        rejectSql("DELETE FROM incident_comments WHERE id = '" + id + "'");
        assertThat(jdbc.queryForObject("SELECT comment FROM incident_comments WHERE id = ?", String.class, id)).isEqualTo("Original");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentEditsHaveOneWinnerAndPreserveBothOriginalAndWinningRevision() throws Exception {
        UUID id = add("Original");
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Integer> call = () -> {
                start.await();
                return mvc.perform(put(path + "/comments/" + id).with(auth(author, "incident:update"))
                        .contentType(MediaType.APPLICATION_JSON).content(editBody(UUID.randomUUID().toString(), 0)))
                        .andReturn().getResponse().getStatus();
            };
            var first = executor.submit(call); var second = executor.submit(call); start.countDown();
            assertThat(List.of(first.get(20, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(20, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 409);
            assertThat(count("incident_comment_revisions", id)).isEqualTo(2);
            assertThat(count("incident_timeline", id)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT content FROM incident_comment_revisions WHERE comment_id = ? AND revision_number = 0",
                    String.class, id)).isEqualTo("Original");
        } finally { cleanup(); }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void timelineFailureRollsBackCommentAndRevisionTogether() throws Exception {
        UUID id = add("Original");
        try {
            assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(_ -> {
                jdbc.execute("""
                        ALTER TABLE incident_timeline ADD CONSTRAINT test_reject_edit
                        CHECK (event_type <> 'COMMENT_EDITED') NOT VALID
                        """);
                try {
                    mvc.perform(put(path + "/comments/" + id).with(auth(author, "incident:update"))
                            .contentType(MediaType.APPLICATION_JSON).content(editBody("Changed", 0)));
                    entityManager.flush();
                } catch (Exception e) { throw new RuntimeException(e); }
            })).isInstanceOf(RuntimeException.class);
            assertThat(jdbc.queryForObject("SELECT comment FROM incident_comments WHERE id = ?", String.class, id)).isEqualTo("Original");
            assertThat(count("incident_comment_revisions", id)).isEqualTo(1);
            assertThat(count("incident_timeline", id)).isEqualTo(1);
        } finally { cleanup(); }
    }

    private void forceChecks() {
        entityManager.flush();
        jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
        jdbc.execute("SET CONSTRAINTS ALL DEFERRED");
    }

    private void rejectSql(String sql) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            var savepoint = connection.setSavepoint();
            try {
                assertThatThrownBy(() -> {
                    jdbc.execute(sql);
                    jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
                }).isInstanceOf(DataIntegrityViolationException.class);
            } finally {
                connection.rollback(savepoint);
                connection.releaseSavepoint(savepoint);
            }
            return null;
        });
    }

    private long count(String table, UUID commentId) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE comment_id = ?", Long.class, commentId);
    }

    private UUID add(String content) throws Exception {
        var result = mvc.perform(post(path + "/comments").with(auth(author, "incident:update"))
                        .contentType(MediaType.APPLICATION_JSON).content(body(content)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asString());
    }

    private void edit(UUID id, String content, long version, User actor, String authority, int expected) throws Exception {
        mvc.perform(put(path + "/comments/" + id).with(auth(actor, authority)).contentType(MediaType.APPLICATION_JSON)
                .content(editBody(content, version))).andExpect(status().is(expected));
    }

    private String body(String content) { return json.writeValueAsString(Map.of("comment", content)); }
    private String editBody(String content, long version) { return json.writeValueAsString(Map.of("comment", content, "version", version)); }

    private JwtRequestPostProcessor auth(User actor, String authority) {
        return jwt().jwt(j -> j.subject(actor.getId().toString()).claim("org_id", actor.getOrganization().getId().toString()))
                .authorities(new SimpleGrantedAuthority(authority));
    }

    private Organization organization() {
        Organization value = new Organization(); value.setName("Collaboration");
        value.setSlug("collaboration-" + UUID.randomUUID()); value.setStatus(OrganizationStatus.ACTIVE);
        return organizations.saveAndFlush(value);
    }

    private User user(Organization org) {
        User value = new User(); value.setOrganization(org); value.setFirstName("Alice"); value.setLastName("Smith");
        value.setEmail(UUID.randomUUID() + "@example.com"); value.setPasswordHash("not-exposed"); value.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(value);
    }

    private void cleanup() {
        IncidentFixtureCleanup.purge(jdbc, transactions, incident.getId());
        users.deleteById(author.getId());
        organizations.deleteById(organization.getId());
    }
}
