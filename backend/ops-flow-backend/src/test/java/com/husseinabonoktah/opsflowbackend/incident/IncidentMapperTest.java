package com.husseinabonoktah.opsflowbackend.incident;

import com.husseinabonoktah.opsflowbackend.incident.application.IncidentMapper;
import com.husseinabonoktah.opsflowbackend.incident.domain.Incident;
import com.husseinabonoktah.opsflowbackend.incident.domain.Severity;
import com.husseinabonoktah.opsflowbackend.organization.domain.Organization;
import com.husseinabonoktah.opsflowbackend.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class IncidentMapperTest {
    @Test
    void returnsCreatorIdAndFullName() {
        User creator = new User();
        UUID creatorId = UUID.randomUUID();
        ReflectionTestUtils.setField(creator, "id", creatorId);
        creator.setFirstName(" Alice ");
        creator.setLastName(" Smith ");
        Incident incident = Incident.open(new Organization(), "INC-test", "Outage", null, Severity.SEV2, creator);
        var response = new IncidentMapper().toResponse(incident);
        assertThat(response.createdBy()).isEqualTo(creatorId);
        assertThat(response.createdByName()).isEqualTo("Alice Smith");
    }
}
