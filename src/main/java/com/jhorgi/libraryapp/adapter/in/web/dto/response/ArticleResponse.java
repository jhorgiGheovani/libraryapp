package com.jhorgi.libraryapp.adapter.in.web.dto.response;

import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.Visibility;

import java.time.Instant;

/** The brief's field list, plus the visibility flag decided in C1. */
public record ArticleResponse(
        Long id,
        String title,
        String content,
        Long authorId,
        Visibility visibility,
        Instant createdAt,
        Instant updatedAt
) {

    public static ArticleResponse from(Article article) {
        return new ArticleResponse(
                article.id(),
                article.title(),
                article.content(),
                article.authorId(),
                article.visibility(),
                article.createdAt(),
                article.updatedAt()
        );
    }
}
