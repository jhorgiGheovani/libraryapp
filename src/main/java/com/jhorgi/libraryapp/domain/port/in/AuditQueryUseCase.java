package com.jhorgi.libraryapp.domain.port.in;

import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.AuditEntry;
import com.jhorgi.libraryapp.domain.model.AuditSearch;
import com.jhorgi.libraryapp.domain.model.PagedResult;

/**
 * Reading the trail. There is no write use case: nothing outside the audit
 * pipeline may append to it, and nothing at all may edit it.
 */
public interface AuditQueryUseCase {

    PagedResult<AuditEntry> search(AuditSearch criteria, Actor requester, int page, int size);
}
