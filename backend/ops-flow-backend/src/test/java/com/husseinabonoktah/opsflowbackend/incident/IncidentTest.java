package com.husseinabonoktah.opsflowbackend.incident;

import com.husseinabonoktah.opsflowbackend.incident.domain.*;
import com.husseinabonoktah.opsflowbackend.incident.domain.exception.InvalidIncidentTransitionException;
import com.husseinabonoktah.opsflowbackend.organization.domain.Organization;
import com.husseinabonoktah.opsflowbackend.user.domain.User;
import com.husseinabonoktah.opsflowbackend.shared.exception.ResourceConflictException;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.*;

class IncidentTest {
    private final List<Consumer<Incident>> actions = List.of(Incident::acknowledge,
            Incident::startInvestigation, Incident::mitigate, Incident::resolve, Incident::close);

    @Test
    void permitsOnlyTheNextTransitionFromEveryState() {
        for (int state = 0; state < IncidentStatus.values().length; state++) {
            for (int action = 0; action < actions.size(); action++) {
                Incident incident = at(state);
                Consumer<Incident> transition = actions.get(action);
                if (action == state) {
                    transition.accept(incident);
                    assertThat(incident.getStatus()).isEqualTo(IncidentStatus.values()[state + 1]);
                } else {
                    assertThatThrownBy(() -> transition.accept(incident))
                            .isInstanceOf(InvalidIncidentTransitionException.class);
                    assertThat(incident.getStatus()).isEqualTo(IncidentStatus.values()[state]);
                }
            }
        }
    }

    @Test
    void timestampsAreSetOnlyOnSuccessfulTransitionsAndCannotBeOverwritten() {
        Incident incident = at(0);
        assertThat(incident.getAcknowledgedAt()).isNull();
        assertThat(incident.getMitigatedAt()).isNull();
        assertThat(incident.getResolvedAt()).isNull();
        assertThat(incident.getClosedAt()).isNull();
        incident.acknowledge();
        var acknowledged = incident.getAcknowledgedAt();
        assertThatThrownBy(incident::acknowledge).isInstanceOf(InvalidIncidentTransitionException.class);
        assertThat(incident.getAcknowledgedAt()).isEqualTo(acknowledged);
        incident.startInvestigation();
        incident.mitigate();
        incident.resolve();
        incident.close();
        assertThat(incident.getMitigatedAt()).isAfterOrEqualTo(acknowledged);
        assertThat(incident.getResolvedAt()).isAfterOrEqualTo(incident.getMitigatedAt());
        assertThat(incident.getClosedAt()).isAfterOrEqualTo(incident.getResolvedAt());
    }

    @Test
    void closedIncidentsRejectAllEdits() {
        Incident incident = at(5);
        assertThatThrownBy(() -> incident.updateDetails("changed", null)).isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> incident.changeSeverity(Severity.SEV1)).isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> incident.assign(null, null)).isInstanceOf(ResourceConflictException.class);
    }

    private Incident at(int state) {
        Incident incident = Incident.open(new Organization(), "INC-test", "Outage", null, Severity.SEV2, new User());
        for (int step = 0; step < state; step++) actions.get(step).accept(incident);
        return incident;
    }
}
