package com.jhorgi.libraryapp.fake;

import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.model.Permission;
import com.jhorgi.libraryapp.domain.model.Visibility;
import com.jhorgi.libraryapp.domain.port.out.ArticleRepositoryPort;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class FakeArticleRepository implements ArticleRepositoryPort {

    private final Map<Long, Article> byId = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);
    // Fixed clock steps: real timestamps in a fast test can collide and make
    // ordering assertions flaky.
    private final AtomicLong clock = new AtomicLong(1_000);

    @Override
    public Article save(Article article) {
        Instant now = Instant.ofEpochSecond(clock.incrementAndGet());
        Long id = article.id() != null ? article.id() : sequence.incrementAndGet();
        Instant createdAt = article.createdAt() != null ? article.createdAt() : now;

        Article stored = new Article(id, article.title(), article.content(), article.authorId(),
                article.visibility(), createdAt, now);
        byId.put(id, stored);
        return stored;
    }

    @Override
    public Optional<Article> findById(Long articleId) {
        return Optional.ofNullable(byId.get(articleId));
    }

    @Override
    public PagedResult<Article> findVisibleTo(Actor viewer, int page, int size) {
        List<Article> visible = byId.values().stream()
                .filter(a -> a.visibility() == Visibility.PUBLIC
                        || viewer.owns(a)
                        || viewer.can(Permission.ARTICLE_READ_ALL))
                .sorted(Comparator.comparing(Article::createdAt).reversed()
                        .thenComparing(Comparator.comparing(Article::id).reversed()))
                .toList();

        int from = Math.min(page * size, visible.size());
        int to = Math.min(from + size, visible.size());
        return new PagedResult<>(visible.subList(from, to), page, size, visible.size());
    }

    @Override
    public void deleteById(Long articleId) {
        byId.remove(articleId);
    }

    @Override
    public void deleteByAuthorId(Long authorId) {
        byId.values().removeIf(a -> a.isOwnedBy(authorId));
    }
}
