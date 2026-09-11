package com.jhorgi.libraryapp.domain.port.in;

import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.Visibility;


public interface ArticleCommandUseCase {

    Article create(CreateArticleCommand command);

    Article update(UpdateArticleCommand command);

    void delete(Long articleId, Actor requester);


    record CreateArticleCommand(String title, String content, Visibility visibility, Actor author)
            implements AuditableCommand {

        /** The author of a new article <em>is</em> the caller; slice 4 named the field for the role it plays. */
        @Override
        public Actor requester() {
            return author;
        }
    }

    record UpdateArticleCommand(Long articleId, String title, String content, Visibility visibility,
                                Actor requester) implements AuditableCommand {
    }
}
