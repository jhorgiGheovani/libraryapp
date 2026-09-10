package com.jhorgi.libraryapp.application;

import com.jhorgi.libraryapp.application.article.ArticleQueryService;
import com.jhorgi.libraryapp.domain.exception.ArticleNotFoundException;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.model.Visibility;
import com.jhorgi.libraryapp.fake.FakeArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleQueryServiceTest {

    private static final Long ALICE = 1L;
    private static final Long BOB = 2L;

    private FakeArticleRepository articles;
    private ArticleQueryService service;

    @BeforeEach
    void setUp() {
        articles = new FakeArticleRepository();
        service = new ArticleQueryService(articles);
    }

    private Article save(String title, Long authorId, Visibility visibility) {
        return articles.save(Article.newArticle(title, "Content", authorId, visibility));
    }

    @Test
    void anyoneCanReadAPublicArticle() {
        Article article = save("Public", ALICE, Visibility.PUBLIC);

        assertEquals("Public", service.getById(article.id(), BOB).title());
    }

    @Test
    void ownerCanReadTheirOwnPrivateArticle() {
        Article article = save("Draft", ALICE, Visibility.PRIVATE);

        assertEquals("Draft", service.getById(article.id(), ALICE).title());
    }

    @Test
    void othersPrivateArticleIsNotFound() {
        Article article = save("Draft", ALICE, Visibility.PRIVATE);

        assertThrows(ArticleNotFoundException.class, () -> service.getById(article.id(), BOB));
    }

    @Test
    void unknownIdIsNotFound() {
        assertThrows(ArticleNotFoundException.class, () -> service.getById(999L, ALICE));
    }

    @Test
    void listShowsPublicArticlesAndOwnDraftsOnly() {
        save("Alice public", ALICE, Visibility.PUBLIC);
        save("Alice draft", ALICE, Visibility.PRIVATE);
        save("Bob public", BOB, Visibility.PUBLIC);
        save("Bob draft", BOB, Visibility.PRIVATE);

        PagedResult<Article> page = service.list(BOB, 0, 10);

        List<String> titles = page.items().stream().map(Article::title).toList();
        assertTrue(titles.containsAll(List.of("Alice public", "Bob public", "Bob draft")));
        assertEquals(3, titles.size(), "Alice's draft must not appear");
    }

    @Test
    void totalCountExcludesArticlesTheViewerCannotSee() {
        // The total comes from the same filtered query, so it must not betray
        // how many hidden articles exist.
        save("Alice draft one", ALICE, Visibility.PRIVATE);
        save("Alice draft two", ALICE, Visibility.PRIVATE);
        save("Bob public", BOB, Visibility.PUBLIC);

        PagedResult<Article> page = service.list(BOB, 0, 10);

        assertEquals(1, page.totalItems());
        assertEquals(1, page.totalPages());
    }

    @Test
    void pagingReturnsDisjointPages() {
        for (int i = 0; i < 5; i++) {
            save("Article " + i, ALICE, Visibility.PUBLIC);
        }

        PagedResult<Article> first = service.list(BOB, 0, 2);
        PagedResult<Article> second = service.list(BOB, 1, 2);

        assertEquals(2, first.items().size());
        assertEquals(2, second.items().size());
        assertEquals(5, first.totalItems());
        assertEquals(3, first.totalPages());
        assertTrue(first.items().stream().map(Article::id)
                .noneMatch(id -> second.items().stream().anyMatch(a -> a.id().equals(id))));
    }

    @Test
    void newestArticlesComeFirst() {
        save("Oldest", ALICE, Visibility.PUBLIC);
        save("Middle", ALICE, Visibility.PUBLIC);
        save("Newest", ALICE, Visibility.PUBLIC);

        PagedResult<Article> page = service.list(BOB, 0, 10);

        assertEquals(List.of("Newest", "Middle", "Oldest"),
                page.items().stream().map(Article::title).toList());
    }
}
