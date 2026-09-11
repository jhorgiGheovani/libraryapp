package com.jhorgi.libraryapp.domain.model;

import java.time.Instant;

/**
 * One row of the trail: an {@link AuditRecord} plus everything the transport
 * contributed.
 *
 * <p>A dozen positional fields is exactly the shape a constructor call gets
 * unreadable at — {@code (null, 4L, EDITOR, ...)} tells a reader nothing — so
 * construction goes through {@link Builder}.
 *
 * <p>{@code actorRole} is stored on the row rather than joined from
 * {@code users}: the trail must say what the role <em>was</em> at the time, and
 * the account may since have been demoted or deleted outright.
 */
public record AuditEntry(
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Long id;
        private Long actorId;
        private Role actorRole;
        private AuditAction action;
        private AuditTargetType targetType;
        private AuditOutcome outcome;
        private String detail;
        private String ipAddress;
        private String userAgent;
        private String browser = DeviceInfo.UNKNOWN;
        private String operatingSystem = DeviceInfo.UNKNOWN;
        private String device = DeviceInfo.UNKNOWN;
        private Instant occurredAt;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        /** Copies the five fields the application layer owns. */
        public Builder from(AuditRecord record) {
            this.actorId = record.actorId();
            this.actorRole = record.actorRole();
            this.action = record.action();
            this.targetType = record.targetType();
            this.outcome = record.outcome();
            this.detail = record.detail();
            return this;
        }

        public Builder actorId(Long actorId) {
            this.actorId = actorId;
            return this;
        }

        public Builder actorRole(Role actorRole) {
            this.actorRole = actorRole;
            return this;
        }

        public Builder action(AuditAction action) {
            this.action = action;
            return this;
        }

        public Builder targetType(AuditTargetType targetType) {
            this.targetType = targetType;
            return this;
        }

        public Builder outcome(AuditOutcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder device(DeviceInfo info) {
            this.browser = info.browser();
            this.operatingSystem = info.operatingSystem();
            this.device = info.device();
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public AuditEntry build() {
            return new AuditEntry(id, actorId, actorRole, action, targetType, outcome,
                    detail, ipAddress, userAgent, browser, operatingSystem, device, occurredAt);
        }
    }
}
