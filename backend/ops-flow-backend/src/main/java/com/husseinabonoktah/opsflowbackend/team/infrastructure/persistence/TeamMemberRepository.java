package com.husseinabonoktah.opsflowbackend.team.infrastructure.persistence;

import com.husseinabonoktah.opsflowbackend.team.domain.TeamMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {
    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    boolean existsByUserIdAndPrimaryTrue(UUID userId);

    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    @EntityGraph(attributePaths = {"user", "team"})
    Page<TeamMember> findAllByTeamId(UUID teamId, Pageable pageable);
}
