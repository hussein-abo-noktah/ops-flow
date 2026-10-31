package com.husseinabonoktah.opsflowbackend.incident.infrastructure.persistence;

import com.husseinabonoktah.opsflowbackend.incident.domain.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.*;
import java.util.*;
public interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {

    @Query(value = "SELECT nextval('incident_number_seq')", nativeQuery = true)
    long nextIncidentNumber();

    Optional<Incident> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Page<Incident> findAllByOrganizationId(UUID organizationId, Pageable pageable);
}
