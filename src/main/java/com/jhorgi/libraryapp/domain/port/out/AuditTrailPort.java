package com.jhorgi.libraryapp.domain.port.out;

import com.jhorgi.libraryapp.domain.model.AuditRecord;

/**
 * The write side of the trail, as the application layer sees it.
 *
 * <p>Separate from {@link AuditLogPort} on purpose. This one is fire-and-forget
 * and is called from the hot path of a request; the other one is the storage
 * contract, synchronous, and is only reached from the background listener and
 * the query service. Collapsing them into one interface would put
 * {@code search} in front of every caller that only ever writes.
 *
 * <p><strong>Implementations must not throw.</strong> Audit is an observer of
 * the request, never a participant in it: a trail that cannot be written is a
 * problem for operations, not a reason to fail a user's delete.
 */
public interface AuditTrailPort {

    void record(AuditRecord record);
}
