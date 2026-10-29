package com.husseinabonoktah.opsflowbackend.incident.infrastructure.persistence;

import com.husseinabonoktah.opsflowbackend.incident.domain.IncidentCommentRevision;
import org.springframework.data.domain.*;
import org.springframework.data.repository.Repository;
import java.util.UUID;

public interface IncidentCommentRevisionRepository extends Repository<IncidentCommentRevision, UUID> {
    IncidentCommentRevision save(IncidentCommentRevision revision);
    Page<IncidentCommentRevision> findAllByCommentIdAndCommentIncidentOrganizationId(
            UUID commentId, UUID organizationId, Pageable pageable);
}
