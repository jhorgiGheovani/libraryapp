package com.jhorgi.libraryapp.adapter.out.persistence.repository;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.ArticleEntity;
import com.jhorgi.libraryapp.domain.model.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleJpaRepository extends JpaRepository<ArticleEntity, Long> {

    /**
     * Derives {@code WHERE visibility = ?1 OR author_id = ?2}. The adapter always
     * passes PUBLIC, so a viewer sees public articles plus their own drafts —
     * enforced in SQL, so COUNT and LIMIT both respect it.
     */
    Page<ArticleEntity> findByVisibilityOrAuthorId(Visibility visibility, Long authorId, Pageable pageable);
}
