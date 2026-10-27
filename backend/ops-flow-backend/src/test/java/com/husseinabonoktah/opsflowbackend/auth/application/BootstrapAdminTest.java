package com.husseinabonoktah.opsflowbackend.auth.application;

import com.husseinabonoktah.opsflowbackend.user.infrastructure.persistence.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BootstrapAdminTest {
    @Test
    void rejectsInvalidConfigurationBeforeCreatingAnyRecords() {
        var users = mock(UserRepository.class);
        var entityManager = mock(EntityManager.class);
        when(entityManager.createNativeQuery("LOCK TABLE users IN EXCLUSIVE MODE")).thenReturn(mock(Query.class));
        var encoder = mock(PasswordEncoder.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            for (String[] input : new String[][]{
                    {"", "long-enough-password"}, {"invalid", "long-enough-password"},
                    {"admin@example.com", ""}, {"admin@example.com", "too-short"},
                    {"admin@example.com", "a".repeat(73)}, {"admin@example.com", "é".repeat(37)}}) {
                var bootstrap = new BootstrapAdmin(users, entityManager, encoder,
                        factory.getValidator(), input[0], input[1]);
                assertThatThrownBy(() -> bootstrap.run(new DefaultApplicationArguments()))
                        .isInstanceOf(IllegalStateException.class);
            }
        }
        verify(entityManager, never()).persist(any());
        verify(users, never()).saveAndFlush(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void existingUsersSkipProvisioningEvenWhenBootstrapCredentialsHaveBeenRemoved() {
        var users = mock(UserRepository.class);
        when(users.count()).thenReturn(1L);
        var entityManager = mock(EntityManager.class);
        when(entityManager.createNativeQuery("LOCK TABLE users IN EXCLUSIVE MODE")).thenReturn(mock(Query.class));
        var encoder = mock(PasswordEncoder.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            new BootstrapAdmin(users, entityManager, encoder, factory.getValidator(), "", "")
                    .run(new DefaultApplicationArguments());
        }
        verify(users, never()).saveAndFlush(any());
        verify(entityManager, never()).persist(any());
        verifyNoInteractions(encoder);
    }
}
