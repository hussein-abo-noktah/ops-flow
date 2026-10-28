package com.husseinabonoktah.opsflowbackend.team.application;

import com.husseinabonoktah.opsflowbackend.shared.exception.*;
import com.husseinabonoktah.opsflowbackend.team.api.request.AddTeamMemberRequest;
import com.husseinabonoktah.opsflowbackend.team.api.response.TeamMemberResponse;
import com.husseinabonoktah.opsflowbackend.team.domain.*;
import com.husseinabonoktah.opsflowbackend.team.infrastructure.persistence.TeamMemberRepository;
import com.husseinabonoktah.opsflowbackend.user.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamMemberService {
    private final TeamMemberRepository repository;
    private final TeamService teams;
    private final TeamMapper mapper;
    private final EntityManager entityManager;

    @Transactional
    public TeamMemberResponse add(UUID teamId, AddTeamMemberRequest request) {
        Team team = teams.findById(teamId);
        // Serialize additions with team status changes, then with memberships for this user.
        entityManager.refresh(team, LockModeType.PESSIMISTIC_WRITE);
        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new ResourceConflictException("Members can only be added to active teams.");
        }
        var users = entityManager.createQuery("""
                select u from User u where u.id = :id and u.organization.id = :organizationId
                """, User.class)
                .setParameter("id", request.userId())
                .setParameter("organizationId", team.getOrganization().getId())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (users.isEmpty()) {
            throw new ResourceNotFoundException("User not found: " + request.userId());
        }
        User user = users.getFirst();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResourceConflictException("Only active users can join a team.");
        }
        if (repository.existsByTeamIdAndUserId(teamId, user.getId())) {
            throw new ResourceConflictException("User is already a member of this team.");
        }
        if (request.primary() && repository.existsByUserIdAndPrimaryTrue(user.getId())) {
            throw new ResourceConflictException("User already has a primary team.");
        }
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        member.setPrimary(request.primary());
        member.setJoinedAt(Instant.now());
        return mapper.toResponse(repository.saveAndFlush(member));
    }

    public Page<TeamMemberResponse> getAll(UUID teamId, int page, int size) {
        teams.findById(teamId);
        return repository.findAllByTeamId(teamId,
                PageRequest.of(page, size, Sort.by("joinedAt", "id"))).map(mapper::toResponse);
    }

    @Transactional
    public void remove(UUID teamId, UUID userId) {
        teams.findById(teamId);
        var member = repository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Team member not found: " + userId));
        repository.delete(member);
        repository.flush();
    }
}
