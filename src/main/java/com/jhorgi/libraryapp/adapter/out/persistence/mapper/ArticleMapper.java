package com.jhorgi.libraryapp.adapter.out.persistence.mapper;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.ArticleEntity;
import com.jhorgi.libraryapp.domain.model.Article;

public final class ArticleMapper {

    private ArticleMapper() {
    }

    public static Article toDomain(ArticleEntity entity) {
        return new Article(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getAuthorId(),
                entity.getVisibility(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static ArticleEntity toEntity(Article article) {
        return new ArticleEntity(
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
