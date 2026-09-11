package com.jhorgi.libraryapp.adapter.out.persistence;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.AuditLogEntity;
import com.jhorgi.libraryapp.adapter.out.persistence.mapper.AuditLogMapper;
import com.jhorgi.libraryapp.adapter.out.persistence.repository.AuditLogJpaRepository;
import com.jhorgi.libraryapp.domain.model.AuditEntry;
import com.jhorgi.libraryapp.domain.model.AuditSearch;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.port.out.AuditLogPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuditLogPersistenceAdapter implements AuditLogPort {

    /**
     * Newest first, with the id as a tiebreak. Entries written in the same
     * millisecond are common — one request can produce several — and without the
     * second key their order across pages is whatever the database felt like,
     * which can show the same row twice and hide another.
     */
    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "id"));

    private final AuditLogJpaRepository jpa;

    public AuditLogPersistenceAdapter(AuditLogJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AuditEntry save(AuditEntry entry) {
        AuditLogEntity saved = jpa.save(AuditLogMapper.toEntity(entry));
        return AuditLogMapper.toDomain(saved);
    }

    @Override
    public PagedResult<AuditEntry> search(AuditSearch criteria, int page, int size) {
        Page<AuditLogEntity> found = jpa.search(criteria, PageRequest.of(page, size, NEWEST_FIRST));
        List<AuditEntry> items = found.getContent().stream().map(AuditLogMapper::toDomain).toList();
        return new PagedResult<>(items, page, size, found.getTotalElements());
    }
}
