package com.husseinabonoktah.opsflowbackend.team.application;

import com.husseinabonoktah.opsflowbackend.organization.domain.Organization;
import com.husseinabonoktah.opsflowbackend.team.api.request.*;
import com.husseinabonoktah.opsflowbackend.team.api.response.*;
import com.husseinabonoktah.opsflowbackend.team.domain.*;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {
    public Team toEntity(CreateTeamRequest request, Organization organization) {
        Team team = new Team();
        team.setOrganization(organization);
        team.setName(request.name());
        team.setCode(request.code());
        team.setDescription(request.description());
        team.setStatus(TeamStatus.ACTIVE);
        return team;
    }

    public void updateEntity(Team team, UpdateTeamRequest request) {
        team.setName(request.name());
        team.setCode(request.code());
        team.setDescription(request.description());
        team.setStatus(request.status());
    }

    public TeamResponse toResponse(Team team) {
        return new TeamResponse(team.getId(), team.getOrganization().getId(), team.getName(),
                team.getCode(), team.getDescription(), team.getStatus(), team.getCreatedAt(), team.getUpdatedAt());
    }

    public TeamMemberResponse toResponse(TeamMember member) {
        var user = member.getUser();
        return new TeamMemberResponse(member.getId(), member.getTeam().getId(), user.getId(),
                user.getFirstName(), user.getLastName(), user.getEmail(), member.isPrimary(), member.getJoinedAt());
    }
}
