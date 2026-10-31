package com.husseinabonoktah.opsflowbackend.incident.infrastructure.persistence.specification;

import com.husseinabonoktah.opsflowbackend.incident.domain.Incident;
import com.husseinabonoktah.opsflowbackend.incident.domain.filter.IncidentFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record IncidentSpecification(IncidentFilter filter, UUID organizationId) implements Specification<Incident> {

    public IncidentSpecification {
        Objects.requireNonNull(filter, "Incident filter is required.");
        Objects.requireNonNull(organizationId, "Organization scope is required.");
    }

    @Override
    public Predicate toPredicate(@NonNull Root<Incident> root,
                                 @Nullable CriteriaQuery<?> query,
                                 @NonNull CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("organization").get("id"), organizationId));

        if (filter.q() != null && !filter.q().isBlank()) {
            String pattern = "%" + filter.q().strip().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), pattern, '\\'),
                    cb.like(cb.lower(root.get("description")), pattern, '\\'),
                    cb.like(cb.lower(root.get("incidentNumber")), pattern, '\\')));
        }

        if (filter.status() != null) {
            predicates.add(cb.equal(root.get("status"), filter.status()));
        }

        if (filter.severity() != null) {
            predicates.add(cb.equal(root.get("severity"), filter.severity()));
        }

        if (filter.assignedTeamId() != null) {
            predicates.add(cb.equal(root.get("assignedTeam").get("id"), filter.assignedTeamId()));
        }

        if (filter.assignedUserId() != null) {
            predicates.add(cb.equal(root.get("assignedUser").get("id"), filter.assignedUserId()));
        }

        if (filter.createdBy() != null) {
            predicates.add(cb.equal(root.get("createdBy").get("id"), filter.createdBy()));
        }

        if (filter.createdFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.createdFrom()));
        }

        if (filter.createdTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.createdTo()));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
