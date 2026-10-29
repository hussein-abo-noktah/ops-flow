package com.husseinabonoktah.opsflowbackend.incident.infrastructure.persistence;

import com.husseinabonoktah.opsflowbackend.incident.domain.IncidentComment;
import org.springframework.data.domain.*;
import org.springframework.data.repository.Repository;
import java.util.*;

public interface IncidentCommentRepository extends Repository<IncidentComment, UUID> {
    IncidentComment saveAndFlush(IncidentComment comment);
    Optional<IncidentComment> findByIdAndIncidentIdAndIncidentOrganizationId(UUID id, UUID incidentId, UUID organizationId);
    Page<IncidentComment> findAllByIncidentIdAndIncidentOrganizationId(UUID incidentId, UUID organizationId, Pageable pageable);
}
