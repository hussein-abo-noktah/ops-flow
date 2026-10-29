package com.husseinabonoktah.opsflowbackend.incident.infrastructure.persistence;

import com.husseinabonoktah.opsflowbackend.incident.domain.IncidentTimeline;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import com.husseinabonoktah.opsflowbackend.incident.domain.TimelineEventType;
import java.time.Instant;
import org.springframework.data.domain.*;
import java.util.UUID;
public interface IncidentTimelineRepository extends Repository<IncidentTimeline, UUID> {
    IncidentTimeline save(IncidentTimeline event);
    Page<IncidentTimeline> findAllByIncidentIdAndIncidentOrganizationId(
            UUID incidentId, UUID organizationId, Pageable pageable);

    @Query("""
            select e from IncidentTimeline e
            where e.incident.id = :incidentId and e.incident.organization.id = :organizationId
              and (:eventType is null or e.eventType = :eventType)
              and (:actorId is null or e.actor.id = :actorId)
              and (:commentId is null or e.comment.id = :commentId)
              and (cast(:from as Instant) is null or e.createdAt >= :from)
              and (cast(:to as Instant) is null or e.createdAt <= :to)
            """)
    Page<IncidentTimeline> search(UUID incidentId, UUID organizationId, TimelineEventType eventType,
                                  UUID actorId, UUID commentId, Instant from, Instant to, Pageable pageable);
}
