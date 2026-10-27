package com.husseinabonoktah.opsflowbackend.organization.infrastructure.persistence;

import com.husseinabonoktah.opsflowbackend.organization.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);
}
