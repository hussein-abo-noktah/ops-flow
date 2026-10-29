package com.husseinabonoktah.opsflowbackend.shared;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;

/** Owner-only fixture cleanup in the disposable Testcontainers database.
 * Trigger changes and deletion are transactional, so failures restore protection. */
public final class IncidentFixtureCleanup {
    private IncidentFixtureCleanup() {}

    public static void purge(JdbcTemplate jdbc, PlatformTransactionManager transactions, UUID incidentId) {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            jdbc.execute("ALTER TABLE incident_timeline DISABLE TRIGGER timeline_no_mutation");
            jdbc.execute("ALTER TABLE incident_comment_revisions DISABLE TRIGGER comment_revisions_no_mutation");
            jdbc.update("DELETE FROM incident_timeline WHERE incident_id = ?", incidentId);
            jdbc.update("""
                    DELETE FROM incident_comment_revisions WHERE comment_id IN
                    (SELECT id FROM incident_comments WHERE incident_id = ?)
                    """, incidentId);
            jdbc.update("DELETE FROM incident_comments WHERE incident_id = ?", incidentId);
            jdbc.update("DELETE FROM incidents WHERE id = ?", incidentId);
            jdbc.execute("ALTER TABLE incident_comment_revisions ENABLE TRIGGER comment_revisions_no_mutation");
            jdbc.execute("ALTER TABLE incident_timeline ENABLE TRIGGER timeline_no_mutation");
        });
    }
}
