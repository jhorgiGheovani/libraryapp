package com.jhorgi.libraryapp.application.article;

import com.jhorgi.libraryapp.application.policy.ArticlePolicy;
import com.jhorgi.libraryapp.audit.Auditable;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.port.in.ArticleQueryUseCase;
import com.jhorgi.libraryapp.domain.port.out.ArticleRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class ArticleQueryService implements ArticleQueryUseCase {

    private final ArticleRepositoryPort articles;

    public ArticleQueryService(ArticleRepositoryPort articles) {
        this.articles = articles;
    }

    /**
     * Audited on both outcomes, and the failure is the interesting one: a Viewer
     * who is refused a private article gets a 404 that tells them nothing, while
     * the trail records that the id was probed at all. Enumeration looks like
     * noise one request at a time and like a pattern in this table.
     */
    @Override
    @Auditable(action = AuditAction.ARTICLE_READ, target = AuditTargetType.ARTICLE)
    public Article getById(Long articleId, Actor requester) {
        return ArticlePolicy.readable(articles.findById(articleId), requester);
    }

    @Override
    @Auditable(action = AuditAction.ARTICLE_LIST, target = AuditTargetType.ARTICLE)
    public PagedResult<Article> list(Actor requester, int page, int size) {
        return articles.findVisibleTo(requester, page, size);
    }
}
