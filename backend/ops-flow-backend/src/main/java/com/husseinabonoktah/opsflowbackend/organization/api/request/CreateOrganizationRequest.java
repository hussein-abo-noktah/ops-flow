package com.husseinabonoktah.opsflowbackend.organization.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 100)
        @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*",
                message = "must contain lowercase letters or digits separated by single hyphens")
        String slug
) {
    public CreateOrganizationRequest {
        name = name == null ? null : name.strip();
        slug = slug == null ? null : slug.strip();
    }
}
