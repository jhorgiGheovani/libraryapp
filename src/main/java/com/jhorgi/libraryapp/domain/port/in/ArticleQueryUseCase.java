package com.jhorgi.libraryapp.domain.port.in;

import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.PagedResult;

public interface ArticleQueryUseCase {

    Article getById(Long articleId, Actor requester);

    PagedResult<Article> list(Actor requester, int page, int size);
}
