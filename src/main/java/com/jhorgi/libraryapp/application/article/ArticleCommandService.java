package com.jhorgi.libraryapp.application.article;

import com.jhorgi.libraryapp.application.policy.ArticlePolicy;
import com.jhorgi.libraryapp.audit.Auditable;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;
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
    @Auditable(action = AuditAction.ARTICLE_CREATE, target = AuditTargetType.ARTICLE)
    public Article create(CreateArticleCommand command) {
        ArticlePolicy.requireCanCreate(command.author());

        Visibility visibility = command.visibility() != null ? command.visibility() : DEFAULT_VISIBILITY;
        return articles.save(Article.newArticle(
                command.title(), command.content(), command.author().id(), visibility));
    }

    @Override
    @Auditable(action = AuditAction.ARTICLE_UPDATE, target = AuditTargetType.ARTICLE)
    public Article update(UpdateArticleCommand command) {
        Actor requester = command.requester();
        Article article = readable(command.articleId(), requester);
        ArticlePolicy.requireCanUpdate(article, requester);

        Visibility visibility = command.visibility() != null ? command.visibility() : article.visibility();
        return articles.save(article.withRevision(command.title(), command.content(), visibility));
    }

    @Override
    @Auditable(action = AuditAction.ARTICLE_DELETE, target = AuditTargetType.ARTICLE)
    public void delete(Long articleId, Actor requester) {
        Article article = readable(articleId, requester);
        ArticlePolicy.requireCanDelete(article, requester);
        articles.deleteById(articleId);
    }

    private Article readable(Long articleId, Actor requester) {
        return ArticlePolicy.readable(articles.findById(articleId), requester);
    }
}
