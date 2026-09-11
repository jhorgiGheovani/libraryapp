package com.jhorgi.libraryapp.adapter.out.persistence.repository;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.AuditLogEntity;
import com.jhorgi.libraryapp.domain.model.AuditSearch;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates {@link AuditSearch} into SQL predicates.
 *
 * <p>Built as a Specification rather than one JPQL query full of
 * {@code (:action is null or a.action = :action)}. That pattern reads well and
 * has a trap in it: binding a null enum leaves Hibernate with no type to infer,
 * and PostgreSQL then rejects the statement — a failure that H2 does not
 * reproduce, so the tests would pass and production would 500. Omitting the
 * predicate entirely cannot fail that way, and produces a tighter query.
 */
final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    static Specification<AuditLogEntity> matching(AuditSearch criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.actorId() != null) {
                predicates.add(builder.equal(root.get("actorId"), criteria.actorId()));
            }
            if (criteria.action() != null) {
                predicates.add(builder.equal(root.get("action"), criteria.action()));
            }
            if (criteria.targetType() != null) {
                predicates.add(builder.equal(root.get("targetType"), criteria.targetType()));
            }
            if (criteria.outcome() != null) {
                predicates.add(builder.equal(root.get("outcome"), criteria.outcome()));
            }
            // Inclusive at both ends: a caller asking for "09:00 to 10:00" means
            // the hour, and an exclusive bound would silently drop an entry that
            // landed exactly on it.
            if (criteria.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), criteria.from()));
            }
            if (criteria.to() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("occurredAt"), criteria.to()));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
