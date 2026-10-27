package com.husseinabonoktah.opsflowbackend.user.api.request;

import com.husseinabonoktah.opsflowbackend.user.domain.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Locale;
import java.util.UUID;

public record UpdateUserRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull UserStatus status
) {
    public UpdateUserRequest {
        firstName = firstName == null ? null : firstName.strip();
        lastName = lastName == null ? null : lastName.strip();
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
