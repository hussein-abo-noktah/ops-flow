package com.husseinabonoktah.opsflowbackend.auth.application;

import com.husseinabonoktah.opsflowbackend.auth.api.request.LoginRequest;
import com.husseinabonoktah.opsflowbackend.auth.domain.Role;
import com.husseinabonoktah.opsflowbackend.organization.domain.Organization;
import com.husseinabonoktah.opsflowbackend.organization.domain.OrganizationStatus;
import com.husseinabonoktah.opsflowbackend.user.domain.User;
import com.husseinabonoktah.opsflowbackend.user.domain.UserStatus;
import com.husseinabonoktah.opsflowbackend.user.infrastructure.persistence.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.bootstrap", name = "enabled", havingValue = "true")
public class BootstrapAdmin implements ApplicationRunner {
    private final UserRepository users;
    private final EntityManager entityManager;
    private final PasswordEncoder passwords;
    private final Validator validator;
    private final String email;
    private final String password;

    public BootstrapAdmin(UserRepository users, EntityManager entityManager, PasswordEncoder passwords,
                          Validator validator, @Value("${app.bootstrap.email:}") String email,
                          @Value("${app.bootstrap.password:}") String password) {
        this.users = users;
        this.entityManager = entityManager;
        this.passwords = passwords;
        this.validator = validator;
        this.email = email.strip().toLowerCase(Locale.ROOT);
        this.password = password;
    }

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments arguments) {
        entityManager.createNativeQuery("LOCK TABLE users IN EXCLUSIVE MODE").executeUpdate();
        if (users.count() > 0) return;

        if (!validator.validateValue(LoginRequest.class, "email", email).isEmpty()) {
            throw new IllegalStateException("AUTH_BOOTSTRAP_EMAIL must be a valid email of at most 255 characters");
        }
        if (password.isBlank() || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("AUTH_BOOTSTRAP_PASSWORD must be at least 12 characters and at most 72 UTF-8 bytes");
        }

        Organization organization = new Organization();
        organization.setName("Ops Flow Platform");
        organization.setSlug("platform-" + UUID.randomUUID());
        organization.setStatus(OrganizationStatus.ACTIVE);
        entityManager.persist(organization);

        Role role = Role.platformAdmin(organization);
        entityManager.persist(role);

        User admin = new User();
        admin.setOrganization(organization);
        admin.setFirstName("Platform");
        admin.setLastName("Admin");
        admin.setEmail(email);
        admin.setPasswordHash(passwords.encode(password));
        admin.setStatus(UserStatus.ACTIVE);
        admin.getRoles().add(role);
        users.saveAndFlush(admin);
    }
}
