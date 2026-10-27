package com.husseinabonoktah.opsflowbackend.organization.application;

import com.husseinabonoktah.opsflowbackend.organization.domain.Organization;
import com.husseinabonoktah.opsflowbackend.organization.domain.OrganizationStatus;
import com.husseinabonoktah.opsflowbackend.organization.api.request.CreateOrganizationRequest;
import com.husseinabonoktah.opsflowbackend.organization.api.response.OrganizationResponse;
import com.husseinabonoktah.opsflowbackend.organization.api.request.UpdateOrganizationRequest;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {
    public Organization toEntity(CreateOrganizationRequest request) {
        Organization organization = new Organization();
        organization.setName(request.name());
        organization.setSlug(request.slug());
        organization.setStatus(OrganizationStatus.ACTIVE);
        return organization;
    }

    public void updateEntity(Organization organization, UpdateOrganizationRequest request) {
        organization.setName(request.name());
        organization.setSlug(request.slug());
        organization.setStatus(request.status());
    }

    public OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(organization.getId(), organization.getName(),
                organization.getSlug(), organization.getStatus(),
                organization.getCreatedAt(), organization.getUpdatedAt());
    }
}
