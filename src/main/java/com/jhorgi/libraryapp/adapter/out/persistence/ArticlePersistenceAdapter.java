package com.jhorgi.libraryapp.adapter.out.persistence;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.ArticleEntity;
import com.jhorgi.libraryapp.adapter.out.persistence.mapper.ArticleMapper;
import com.jhorgi.libraryapp.adapter.out.persistence.repository.ArticleJpaRepository;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.model.Permission;
import com.jhorgi.libraryapp.domain.model.Visibility;
import com.jhorgi.libraryapp.domain.port.out.ArticleRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class ArticlePersistenceAdapter implements ArticleRepositoryPort {

    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));

    private final ArticleJpaRepository jpa;

    public ArticlePersistenceAdapter(ArticleJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Article save(Article article) {
        ArticleEntity saved = jpa.save(ArticleMapper.toEntity(article));
        return ArticleMapper.toDomain(saved);
    }

    @Override
    public Optional<Article> findById(Long articleId) {
        return jpa.findById(articleId).map(ArticleMapper::toDomain);
    }

    @Override
    public PagedResult<Article> findVisibleTo(Actor viewer, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, NEWEST_FIRST);
        // Two different queries rather than one query with an in-memory filter:
        // the narrow one still filters in SQL, so LIMIT and COUNT agree and the
        // total never reveals how many drafts other people have.
        Page<ArticleEntity> found = viewer.can(Permission.ARTICLE_READ_ALL)
                ? jpa.findAll(pageRequest)
                : jpa.findByVisibilityOrAuthorId(Visibility.PUBLIC, viewer.id(), pageRequest);

        List<Article> items = found.getContent().stream().map(ArticleMapper::toDomain).toList();
        return new PagedResult<>(items, page, size, found.getTotalElements());
    }

    @Override
    public void deleteById(Long articleId) {
        jpa.deleteById(articleId);
    }

    @Override
    @Transactional
    public void deleteByAuthorId(Long authorId) {
        jpa.deleteByAuthorId(authorId);
    }
}
