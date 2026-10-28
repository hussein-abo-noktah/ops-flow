package com.husseinabonoktah.opsflowbackend.team.infrastructure.persistence;

import com.husseinabonoktah.opsflowbackend.team.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    Optional<Team> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<Team> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);
}
