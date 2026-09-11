package com.jhorgi.libraryapp.domain.port.out;

import com.jhorgi.libraryapp.domain.model.AuditEntry;
import com.jhorgi.libraryapp.domain.model.AuditSearch;
import com.jhorgi.libraryapp.domain.model.PagedResult;

/** Storage for the trail: append, and query it back. There is no update or delete. */
public interface AuditLogPort {

    AuditEntry save(AuditEntry entry);

    PagedResult<AuditEntry> search(AuditSearch criteria, int page, int size);
}
