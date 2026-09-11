package com.jhorgi.libraryapp.domain.port.out;

import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.PagedResult;

import java.util.Optional;

public interface ArticleRepositoryPort {

    Article save(Article article);

    Optional<Article> findById(Long articleId);

    /**
     * The listing the viewer is allowed to see. Takes the whole {@link Actor}
     * because the answer is role-dependent: a role holding ARTICLE_READ_ALL sees
     * every article, everyone else sees public plus their own.
     *
     * <p>Implementations must apply the filter in the query, never after loading:
     * post-filtering breaks paging and leaks the total count.
     */
    PagedResult<Article> findVisibleTo(Actor viewer, int page, int size);

    void deleteById(Long articleId);

    /**
     * Used when an account is deleted: their articles go with them. Modelled as
     * an explicit application rule rather than a DB cascade, so it is visible in
     * the service, testable against a fake, and auditable in slice 6.
     */
    void deleteByAuthorId(Long authorId);
}
