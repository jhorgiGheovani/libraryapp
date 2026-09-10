package com.jhorgi.libraryapp.domain.port.in;

import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.Visibility;


public interface ArticleCommandUseCase {

    Article create(CreateArticleCommand command);

    Article update(UpdateArticleCommand command);

    void delete(Long articleId, Long requesterId);


    record CreateArticleCommand(String title, String content, Visibility visibility, Long authorId) {
    }

    record UpdateArticleCommand(Long articleId, String title, String content, Visibility visibility,
                                Long requesterId) {
    }
}
