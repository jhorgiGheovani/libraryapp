package com.jhorgi.libraryapp.domain.port.in;

import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.PagedResult;

public interface ArticleQueryUseCase {

    Article getById(Long articleId, Long requesterId);

    PagedResult<Article> list(Long requesterId, int page, int size);
}
