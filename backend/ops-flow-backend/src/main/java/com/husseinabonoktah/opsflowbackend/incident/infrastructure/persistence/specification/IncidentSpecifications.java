package com.husseinabonoktah.opsflowbackend.incident.infrastructure.persistence.specification;

import com.husseinabonoktah.opsflowbackend.incident.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class IncidentSpecifications {
    private IncidentSpecifications() {}

    public static Specification<Incident> inOrganization(UUID organizationId) {
        Objects.requireNonNull(organizationId, "Organization scope is required.");
        return (root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId);
    }

    public static Specification<Incident> hasStatus(IncidentStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Incident> hasSeverity(Severity severity) {
        return (root, query, cb) -> severity == null ? cb.conjunction() : cb.equal(root.get("severity"), severity);
    }

    public static Specification<Incident> assignedToTeam(UUID teamId) {
        return (root, query, cb) -> teamId == null ? cb.conjunction()
                : cb.equal(root.get("assignedTeam").get("id"), teamId);
    }

    public static Specification<Incident> assignedToUser(UUID userId) {
        return (root, query, cb) -> userId == null ? cb.conjunction()
                : cb.equal(root.get("assignedUser").get("id"), userId);
    }

    public static Specification<Incident> createdBy(UUID userId) {
        return (root, query, cb) -> userId == null ? cb.conjunction()
                : cb.equal(root.get("createdBy").get("id"), userId);
    }

    public static Specification<Incident> createdFrom(Instant from) {
        return (root, query, cb) -> from == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Incident> createdTo(Instant to) {
        return (root, query, cb) -> to == null ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<Incident> matchingText(String text) {
        if (text == null || text.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + text.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(root.get("description")), pattern, '\\'),
                cb.like(cb.lower(root.get("incidentNumber")), pattern, '\\'));
    }
}
