package com.jhorgi.libraryapp.adapter.out.persistence.entity;

import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditOutcome;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;
import com.jhorgi.libraryapp.domain.model.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One audit row.
 *
 * <p>Two things are deliberately absent. There is no foreign key from
 * {@code actor_id} to {@code users.id}: slice 5b deletes an account together
 * with its articles, and the trail of what that account did has to outlive it —
 * a constraint here would either block the delete or cascade away the evidence.
 * And there is no setter or updatable mapping anywhere: rows are appended and
 * read, never edited.
 *
 * <p>{@code actor_role} is denormalised for the same reason. It records the role
 * held <em>at the time of the action</em>, which a join to {@code users} could
 * not reconstruct after a promotion — or after a deletion.
 */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                // The endpoint's default ordering, and the column every filter narrows on.
                @Index(name = "idx_audit_logs_occurred_at", columnList = "occurred_at"),
                @Index(name = "idx_audit_logs_actor_id", columnList = "actor_id"),
                @Index(name = "idx_audit_logs_action", columnList = "action"),
                @Index(name = "idx_audit_logs_target_type", columnList = "target_type")
        }
)
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null for an unauthenticated action — a failed login has no identity to name. */
    @Column(name = "actor_id")
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", length = 32)
    private Role actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private AuditTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditOutcome outcome;

    @Column(length = 200)
    private String detail;

    /** Sized for IPv6, which is 45 characters at its longest. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(length = 40)
    private String browser;

    @Column(name = "operating_system", length = 40)
    private String operatingSystem;

    @Column(length = 40)
    private String device;

    /** Set by the publisher, not the database: when it happened, not when it was written. */
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditLogEntity() {
    }

    public AuditLogEntity(Long id, Long actorId, Role actorRole, AuditAction action,
                          AuditTargetType targetType, AuditOutcome outcome,
                          String detail, String ipAddress, String userAgent, String browser,
                          String operatingSystem, String device, Instant occurredAt) {
        this.id = id;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.action = action;
        this.targetType = targetType;
        this.outcome = outcome;
        this.detail = detail;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.browser = browser;
        this.operatingSystem = operatingSystem;
        this.device = device;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getActorId() {
        return actorId;
    }

    public Role getActorRole() {
        return actorRole;
    }

    public AuditAction getAction() {
        return action;
    }

    public AuditTargetType getTargetType() {
        return targetType;
    }

    public AuditOutcome getOutcome() {
        return outcome;
    }

    public String getDetail() {
        return detail;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getBrowser() {
        return browser;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public String getDevice() {
        return device;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
