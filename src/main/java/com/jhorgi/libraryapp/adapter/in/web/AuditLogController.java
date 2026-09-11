package com.jhorgi.libraryapp.adapter.in.web;

import com.jhorgi.libraryapp.adapter.in.web.dto.response.ApiResponse;
import com.jhorgi.libraryapp.adapter.in.web.dto.response.AuditLogResponse;
import com.jhorgi.libraryapp.adapter.in.web.dto.response.PageResponse;
import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditEntry;
import com.jhorgi.libraryapp.domain.model.AuditOutcome;
import com.jhorgi.libraryapp.domain.model.AuditSearch;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.port.in.AuditQueryUseCase;
import com.jhorgi.libraryapp.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * The queryable trail the brief asks for (C2). Read-only by design — there is no
 * POST, PUT or DELETE here, and no use case behind them if someone added one.
 *
 * <p>The gate is on the class, as on {@link UserController}: an endpoint added
 * here later cannot be left open by forgetting the annotation.
 */
@RestController
@RequestMapping("/audit-logs")
@PreAuthorize("hasAuthority('AUDIT_READ')")
@Tag(name = "Audit log", description = "Read-only trail of who did what, from where. SUPER_ADMIN only")
public class AuditLogController {

    private final AuditQueryUseCase auditLogs;

    public AuditLogController(AuditQueryUseCase auditLogs) {
        this.auditLogs = auditLogs;
    }

    /**
     * Every filter is optional and they combine with AND. Enum values are parsed
     * by Spring, so {@code ?action=NONSENSE} is a 400 rather than an empty page
     * that looks like "nothing happened".
     *
     * <p>{@code from} and {@code to} are ISO-8601 instants, e.g.
     * {@code 2026-09-11T00:00:00Z}, and both bounds are inclusive.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> search(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditTargetType targetType,
            @RequestParam(required = false) AuditOutcome outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal AuthenticatedUser caller) {

        AuditSearch criteria = new AuditSearch(actorId, action, targetType, outcome, from, to);
        PagedResult<AuditEntry> result = auditLogs.search(criteria, caller.actor(), page, size);

        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result, AuditLogResponse::from)));
    }
}
