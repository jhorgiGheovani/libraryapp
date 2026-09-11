package com.jhorgi.libraryapp.application.article;

import com.jhorgi.libraryapp.application.policy.ArticlePolicy;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Article;
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

    @Override
    public Article getById(Long articleId, Actor requester) {
        return ArticlePolicy.readable(articles.findById(articleId), requester);
    }

    @Override
    public PagedResult<Article> list(Actor requester, int page, int size) {
        return articles.findVisibleTo(requester, page, size);
    }
}
