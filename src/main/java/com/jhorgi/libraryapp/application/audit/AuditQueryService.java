package com.jhorgi.libraryapp.application.audit;

import com.jhorgi.libraryapp.application.policy.AuditPolicy;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.AuditEntry;
import com.jhorgi.libraryapp.domain.model.AuditSearch;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.port.in.AuditQueryUseCase;
import com.jhorgi.libraryapp.domain.port.out.AuditLogPort;
import org.springframework.stereotype.Service;

@Service
public class AuditQueryService implements AuditQueryUseCase {

    private final AuditLogPort auditLog;

    public AuditQueryService(AuditLogPort auditLog) {
        this.auditLog = auditLog;
    }

    @Override
    public PagedResult<AuditEntry> search(AuditSearch criteria, Actor requester, int page, int size) {
        AuditPolicy.requireCanReadAuditLog(requester);
        return auditLog.search(criteria != null ? criteria : AuditSearch.all(), page, size);
    }
}
