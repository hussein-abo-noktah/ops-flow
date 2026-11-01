package com.husseinabonoktah.opsflowbackend.incident;

import com.husseinabonoktah.opsflowbackend.incident.application.IncidentMapper;
import com.husseinabonoktah.opsflowbackend.incident.domain.*;
import com.husseinabonoktah.opsflowbackend.organization.domain.Organization;
import com.husseinabonoktah.opsflowbackend.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class IncidentTimelineMapperTest {
    private final IncidentMapper mapper = new IncidentMapper();

    @Test
    void returnsActorIdAndFullName() {
        User actor = new User();
        UUID actorId = UUID.randomUUID();
        ReflectionTestUtils.setField(actor, "id", actorId);
        actor.setFirstName("Alice");
        actor.setLastName("Smith");
        var response = mapper.toResponse(event(actor));
        assertThat(response.actorId()).isEqualTo(actorId);
        assertThat(response.actorName()).isEqualTo("Alice Smith");
    }

    @Test
    void systemEventsHaveNoActorIdOrName() {
        var response = mapper.toResponse(event(null));
        assertThat(response.actorId()).isNull();
        assertThat(response.actorName()).isNull();
    }

    private IncidentTimeline event(User actor) {
        Incident incident = Incident.open(new Organization(), "INC-test", "Outage", null, Severity.SEV2, new User());
        return new IncidentTimeline(incident, TimelineEventType.INCIDENT_CREATED, actor, null, "OPEN");
    }
}
