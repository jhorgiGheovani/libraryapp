package com.jhorgi.libraryapp.application.article;

import com.jhorgi.libraryapp.domain.exception.ArticleAccessDeniedException;
import com.jhorgi.libraryapp.domain.exception.ArticleNotFoundException;
import com.jhorgi.libraryapp.domain.model.Article;

import java.util.Optional;

final class ArticleAccess {

    private ArticleAccess() {
    }

    static Article readable(Optional<Article> found, Long requesterId) {
        Article article = found.orElseThrow(ArticleNotFoundException::new);
        if (!article.isVisibleTo(requesterId)) {
            throw new ArticleNotFoundException();
        }
        return article;
    }


    static void requireOwner(Article article, Long requesterId) {
        if (!article.isOwnedBy(requesterId)) {
            throw new ArticleAccessDeniedException();
        }
    }
}
