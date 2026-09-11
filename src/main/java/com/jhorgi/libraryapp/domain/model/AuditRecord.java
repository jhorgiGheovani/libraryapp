package com.jhorgi.libraryapp.domain.model;

/**
 * What the application layer knows about an auditable action: who, what, on
 * which target, and whether it worked.
 *
 * <p>Deliberately carries no IP, user-agent or timestamp. Those are properties
 * of the transport, not of the decision, and letting them in here would drag
 * {@code HttpServletRequest} into services that are currently testable with
 * nothing but fakes. The adapter behind {@code AuditTrailPort} adds them.
 */
public record AuditRecord(
        Long actorId,
        Role actorRole,
        AuditAction action,
        AuditTargetType targetType,
        AuditOutcome outcome,
        String detail
) {

    public static AuditRecord success(Actor actor, AuditAction action, AuditTargetType targetType) {
        return of(actor, action, targetType, AuditOutcome.SUCCESS, null);
    }

    public static AuditRecord failure(Actor actor, AuditAction action,
                                      AuditTargetType targetType, String detail) {
        return of(actor, action, targetType, AuditOutcome.FAILURE, detail);
    }

    /** Tolerates a null actor: a failed login has no identity to attribute. */
    public static AuditRecord of(Actor actor, AuditAction action, AuditTargetType targetType,
                                 AuditOutcome outcome, String detail) {
        return new AuditRecord(
                actor != null ? actor.id() : null,
                actor != null ? actor.role() : null,
                action, targetType, outcome, detail);
    }
}
