package com.jhorgi.libraryapp.fake;

import com.jhorgi.libraryapp.domain.model.AuditEntry;
import com.jhorgi.libraryapp.domain.model.AuditSearch;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.port.out.AuditLogPort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory stand-in for the audit table. Mirrors the adapter's contract:
 * filters narrow, the total reflects the filter, and the order is newest first.
 */
public class FakeAuditLog implements AuditLogPort {

    private final List<AuditEntry> entries = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    /** Set to make the next save throw, for the "audit failure breaks nothing" test. */
    private RuntimeException failure;

    @Override
    public AuditEntry save(AuditEntry entry) {
        if (failure != null) {
            throw failure;
        }
        // Same as the real adapter: everything the caller sent, plus a generated id.
        AuditEntry stored = new AuditEntry(
                sequence.incrementAndGet(), entry.actorId(), entry.actorRole(), entry.action(),
                entry.targetType(), entry.outcome(), entry.detail(),
                entry.ipAddress(), entry.userAgent(), entry.browser(), entry.operatingSystem(),
                entry.device(), entry.occurredAt());
        entries.add(stored);
        return stored;
    }

    @Override
    public PagedResult<AuditEntry> search(AuditSearch criteria, int page, int size) {
        List<AuditEntry> matching = entries.stream()
                .filter(e -> matches(e, criteria))
                .sorted(Comparator.comparing(AuditEntry::occurredAt)
                        .thenComparing(AuditEntry::id).reversed())
                .toList();

        int from = Math.min(page * size, matching.size());
        int to = Math.min(from + size, matching.size());
        return new PagedResult<>(matching.subList(from, to), page, size, matching.size());
    }

    private static boolean matches(AuditEntry entry, AuditSearch criteria) {
        return (criteria.actorId() == null || criteria.actorId().equals(entry.actorId()))
                && (criteria.action() == null || criteria.action() == entry.action())
                && (criteria.targetType() == null || criteria.targetType() == entry.targetType())
                && (criteria.outcome() == null || criteria.outcome() == entry.outcome())
                && (criteria.from() == null || !entry.occurredAt().isBefore(criteria.from()))
                && (criteria.to() == null || !entry.occurredAt().isAfter(criteria.to()));
    }

    public void failNextSaveWith(RuntimeException ex) {
        this.failure = ex;
    }

    public List<AuditEntry> entries() {
        return List.copyOf(entries);
    }

    public int size() {
        return entries.size();
    }
}
