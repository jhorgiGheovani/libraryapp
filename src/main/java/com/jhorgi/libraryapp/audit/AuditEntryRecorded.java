package com.jhorgi.libraryapp.audit;

import com.jhorgi.libraryapp.domain.model.AuditEntry;

/**
 * The event that carries a finished entry from the request thread to the writer.
 *
 * <p>It holds the entry complete, already enriched — nothing downstream reads
 * {@link AuditContext} again. That matters because the listener runs on another
 * thread, where the ThreadLocal is empty and, worse, may by then hold a
 * different request's data.
 */
public record AuditEntryRecorded(AuditEntry entry) {
}
