package com.jhorgi.libraryapp.application.article;

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
    public Article getById(Long articleId, Long requesterId) {
        return ArticleAccess.readable(articles.findById(articleId), requesterId);
    }

    @Override
    public PagedResult<Article> list(Long requesterId, int page, int size) {
        return articles.findVisibleTo(requesterId, page, size);
    }
}
