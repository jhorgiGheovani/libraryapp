package com.jhorgi.libraryapp.adapter.in.web.dto.response;

import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditEntry;
import com.jhorgi.libraryapp.domain.model.AuditOutcome;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;
import com.jhorgi.libraryapp.domain.model.Role;

import java.time.Instant;

/**
 * The wire shape of one entry. Browser, OS and device are returned alongside the
 * raw header rather than instead of it: the parse is a convenience for reading,
 * the header is the evidence.
 */
public record AuditLogResponse(
        Long id,
        Long actorId,
        Role actorRole,
        AuditAction action,
        AuditTargetType targetType,
        AuditOutcome outcome,
        String detail,
        String ipAddress,
        String userAgent,
        String browser,
        String operatingSystem,
        String device,
        Instant occurredAt
) {

    public static AuditLogResponse from(AuditEntry entry) {
        return new AuditLogResponse(
                entry.id(), entry.actorId(), entry.actorRole(), entry.action(),
                entry.targetType(), entry.outcome(), entry.detail(),
                entry.ipAddress(), entry.userAgent(), entry.browser(), entry.operatingSystem(),
                entry.device(), entry.occurredAt());
    }
}
