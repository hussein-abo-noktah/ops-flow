package com.husseinabonoktah.opsflowbackend.incident;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class IncidentHistoryMigrationIT {
    @Test
    void upgradesExistingEditedCommentsWithoutInventingTheirOriginalTextOrEditor() {
        try (var postgres = new PostgreSQLContainer("postgres:17-alpine")) {
            postgres.start();
            var source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            Flyway.configure().dataSource(source).target("14").load().migrate();
            var jdbc = new JdbcTemplate(source);
            UUID org = UUID.randomUUID(), user = UUID.randomUUID(), incident = UUID.randomUUID(), comment = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO organizations (id,name,slug,status,created_at,updated_at)
                    VALUES (?,'Legacy','legacy','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    """, org);
            jdbc.update("""
                    INSERT INTO users (id,organization_id,first_name,last_name,email,password_hash,status,created_at,updated_at)
                    VALUES (?,?,'Alice','Smith','legacy@example.com','hash','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    """, user, org);
            jdbc.update("""
                    INSERT INTO incidents (id,organization_id,incident_number,title,severity,status,created_by,created_at,updated_at)
                    VALUES (?,?,'INC-legacy','Legacy incident','SEV1','OPEN',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    """, incident, org, user);
            jdbc.update("""
                    INSERT INTO incident_comments (id,incident_id,user_id,comment,is_edited,created_at,updated_at)
                    VALUES (?,?,?,'Latest known text',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    """, comment, incident, user);
            Flyway.configure().dataSource(source).load().migrate();
            var snapshot = jdbc.queryForMap("SELECT * FROM incident_comment_revisions WHERE comment_id = ?", comment);
            assertThat(snapshot.get("content")).isEqualTo("Latest known text");
            assertThat(snapshot.get("revision_type")).isEqualTo("LEGACY_SNAPSHOT");
            assertThat(snapshot.get("revision_number")).isEqualTo(0L);
            assertThat(snapshot.get("editor_id")).isNull();
            assertThat(jdbc.queryForObject("SELECT version FROM incident_comments WHERE id = ?", Long.class, comment)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM incident_timeline", Long.class)).isZero();
        }
    }
}
