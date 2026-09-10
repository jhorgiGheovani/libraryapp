package com.jhorgi.libraryapp.domain.port.out;

import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.PagedResult;

import java.util.Optional;

public interface ArticleRepositoryPort {

    Article save(Article article);

    Optional<Article> findById(Long articleId);


    PagedResult<Article> findVisibleTo(Long viewerId, int page, int size);

    void deleteById(Long articleId);
}
