package com.jhorgi.libraryapp.fake;

import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditRecord;
import com.jhorgi.libraryapp.domain.port.out.AuditTrailPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Collects what the application layer emitted, so a test can assert on the
 * record without any of the enrichment, event or persistence machinery.
 */
public class FakeAuditTrail implements AuditTrailPort {

    private final List<AuditRecord> records = new ArrayList<>();

    @Override
    public void record(AuditRecord record) {
        records.add(record);
    }

    public List<AuditRecord> records() {
        return List.copyOf(records);
    }

    public List<AuditRecord> recordsOf(AuditAction action) {
        return records.stream().filter(r -> r.action() == action).toList();
    }

    public Optional<AuditRecord> last() {
        return records.isEmpty() ? Optional.empty() : Optional.of(records.get(records.size() - 1));
    }

    public int size() {
        return records.size();
    }

    public void clear() {
        records.clear();
    }
}
