package com.jhorgi.libraryapp.adapter.out.persistence.mapper;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.AuditLogEntity;
import com.jhorgi.libraryapp.domain.model.AuditEntry;
import com.jhorgi.libraryapp.domain.model.DeviceInfo;

public final class AuditLogMapper {

    private AuditLogMapper() {
    }

    public static AuditLogEntity toEntity(AuditEntry entry) {
        return new AuditLogEntity(
                entry.id(), entry.actorId(), entry.actorRole(), entry.action(),
                entry.targetType(), entry.outcome(), entry.detail(),
                entry.ipAddress(), entry.userAgent(), entry.browser(), entry.operatingSystem(),
                entry.device(), entry.occurredAt());
    }

    public static AuditEntry toDomain(AuditLogEntity entity) {
        return AuditEntry.builder()
                .id(entity.getId())
                .actorId(entity.getActorId())
                .actorRole(entity.getActorRole())
                .action(entity.getAction())
                .targetType(entity.getTargetType())
                .outcome(entity.getOutcome())
                .detail(entity.getDetail())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .device(new DeviceInfo(entity.getBrowser(), entity.getOperatingSystem(), entity.getDevice()))
                .occurredAt(entity.getOccurredAt())
                .build();
    }
}
