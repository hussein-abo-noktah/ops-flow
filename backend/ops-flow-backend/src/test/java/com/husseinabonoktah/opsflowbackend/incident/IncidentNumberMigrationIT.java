package com.husseinabonoktah.opsflowbackend.incident;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class IncidentNumberMigrationIT {
    @Test
    void convertsUuidReferencesInCreationOrderAndContinuesAfterExistingNumbers() {
        try (var postgres = new PostgreSQLContainer("postgres:17-alpine")) {
            postgres.start();
            var source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            Flyway.configure().dataSource(source).target("15").load().migrate();
            var jdbc = new JdbcTemplate(source);
            UUID org = UUID.randomUUID(), user = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO organizations (id,name,slug,status,created_at,updated_at)
                    VALUES (?,'Legacy','number-migration','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    """, org);
            jdbc.update("""
                    INSERT INTO users (id,organization_id,first_name,last_name,email,password_hash,status,created_at,updated_at)
                    VALUES (?,?,'Alice','Smith','numbers@example.com','hash','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    """, user, org);
            UUID older = insert(jdbc, org, user, "INC-" + UUID.randomUUID(), "2026-01-01T00:00:00Z");
            UUID newer = insert(jdbc, org, user, "INC-" + UUID.randomUUID(), "2026-02-01T00:00:00Z");
            UUID readable = insert(jdbc, org, user, "INC-000042", "2026-03-01T00:00:00Z");
            UUID custom = insert(jdbc, org, user, "CUSTOM-REFERENCE", "2026-03-01T00:00:00Z");

            Flyway.configure().dataSource(source).load().migrate();

            assertThat(number(jdbc, older)).isEqualTo("INC-000043");
            assertThat(number(jdbc, newer)).isEqualTo("INC-000044");
            assertThat(number(jdbc, readable)).isEqualTo("INC-000042");
            assertThat(number(jdbc, custom)).isEqualTo("CUSTOM-REFERENCE");
            assertThat(jdbc.queryForObject("SELECT nextval('incident_number_seq')", Long.class)).isEqualTo(45L);
            assertThat(jdbc.queryForObject("SELECT version FROM incidents WHERE id = ?", Long.class, older)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM incidents", Long.class)).isEqualTo(4L);
        }
    }

    private UUID insert(JdbcTemplate jdbc, UUID org, UUID user, String number, String createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO incidents (id,organization_id,incident_number,title,severity,status,created_by,created_at,updated_at)
                VALUES (?,?,?,'Existing incident','SEV3','OPEN',?,?::timestamptz,?::timestamptz)
                """, id, org, number, user, createdAt, createdAt);
        return id;
    }

    private String number(JdbcTemplate jdbc, UUID id) {
        return jdbc.queryForObject("SELECT incident_number FROM incidents WHERE id = ?", String.class, id);
    }
}
