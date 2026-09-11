package com.jhorgi.libraryapp.domain.model;

import java.time.Instant;

/**
 * Filter criteria for {@code GET /audit-logs}. Every field is optional; a null
 * means "do not narrow on this", and an all-null search returns the whole trail
 * newest-first.
 *
 * <p>The filtering happens in SQL, never after loading — same rule as the
 * article visibility filter in slice 4. An in-memory filter would break paging
 * (a page of 20 coming back with 3) and report a total that ignores the filter.
 */
public record AuditSearch(
        Long actorId,
        AuditAction action,
        AuditTargetType targetType,
        AuditOutcome outcome,
        Instant from,
        Instant to
) {

    public static AuditSearch all() {
        return new AuditSearch(null, null, null, null, null, null);
    }
}
