package com.husseinabonoktah.opsflowbackend.incident.infrastructure.persistence;

import com.husseinabonoktah.opsflowbackend.incident.domain.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.*;
import java.util.*;
public interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {

    @Query(value = "SELECT nextval('incident_number_seq')", nativeQuery = true)
    long nextIncidentNumber();

    @EntityGraph(attributePaths = "createdBy")
    Optional<Incident> findByIdAndOrganizationId(UUID id, UUID organizationId);
    @EntityGraph(attributePaths = "createdBy")
    Page<Incident> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "createdBy")
    Page<Incident> findAll(Specification<Incident> specification, Pageable pageable);
}
