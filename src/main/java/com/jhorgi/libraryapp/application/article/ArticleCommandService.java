package com.jhorgi.libraryapp.application.article;

import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.Visibility;
import com.jhorgi.libraryapp.domain.port.in.ArticleCommandUseCase;
import com.jhorgi.libraryapp.domain.port.out.ArticleRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class ArticleCommandService implements ArticleCommandUseCase {

    private static final Visibility DEFAULT_VISIBILITY = Visibility.PRIVATE;

    private final ArticleRepositoryPort articles;

    public ArticleCommandService(ArticleRepositoryPort articles) {
        this.articles = articles;
    }

    @Override
    public Article create(CreateArticleCommand command) {

        Visibility visibility = command.visibility() != null ? command.visibility() : DEFAULT_VISIBILITY;
        return articles.save(Article.newArticle(
                command.title(), command.content(), command.authorId(), visibility));
    }

    @Override
    public Article update(UpdateArticleCommand command) {
        Article article = requireOwned(command.articleId(), command.requesterId());
        Visibility visibility = command.visibility() != null ? command.visibility() : article.visibility();
        return articles.save(article.withRevision(command.title(), command.content(), visibility));
    }

    @Override
    public void delete(Long articleId, Long requesterId) {
        requireOwned(articleId, requesterId);
        articles.deleteById(articleId);
    }

    private Article requireOwned(Long articleId, Long requesterId) {
        Article article = ArticleAccess.readable(articles.findById(articleId), requesterId);
        ArticleAccess.requireOwner(article, requesterId);
        return article;
    }
}
