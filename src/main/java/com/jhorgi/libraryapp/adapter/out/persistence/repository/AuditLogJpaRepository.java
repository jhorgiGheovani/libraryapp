package com.jhorgi.libraryapp.adapter.out.persistence.repository;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.AuditLogEntity;
import com.jhorgi.libraryapp.domain.model.AuditSearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Derived queries cannot express six independently optional filters — that is
 * 64 combinations — so the read goes through {@link AuditLogSpecifications}
 * instead, which builds only the predicates that were actually asked for.
 */
public interface AuditLogJpaRepository
        extends JpaRepository<AuditLogEntity, Long>, JpaSpecificationExecutor<AuditLogEntity> {

    /** Keeps the Criteria API behind the repository, where the adapter cannot see it. */
    default Page<AuditLogEntity> search(AuditSearch criteria, Pageable pageable) {
        return findAll(AuditLogSpecifications.matching(criteria), pageable);
    }
}
