package com.jhorgi.libraryapp.application;

import com.jhorgi.libraryapp.application.article.ArticleCommandService;
import com.jhorgi.libraryapp.domain.exception.ArticleAccessDeniedException;
import com.jhorgi.libraryapp.domain.exception.ArticleNotFoundException;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.Visibility;
import com.jhorgi.libraryapp.domain.port.in.ArticleCommandUseCase.CreateArticleCommand;
import com.jhorgi.libraryapp.domain.port.in.ArticleCommandUseCase.UpdateArticleCommand;
import com.jhorgi.libraryapp.fake.FakeArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleCommandServiceTest {

    /**
     * Slice-4 ownership rules are role-independent, so they are pinned with the
     * narrowest roles that still hold the permission under test. The stranger is
     * deliberately a CONTRIBUTOR: without ARTICLE_READ_ALL the 403-vs-404 split
     * is the one slice 4 proved. The role matrix itself lives in
     * {@link ArticleRbacMatrixTest}.
     */
    private static final Actor AUTHOR = new Actor(1L, Role.EDITOR);
    private static final Actor STRANGER = new Actor(2L, Role.CONTRIBUTOR);

    private FakeArticleRepository articles;
    private ArticleCommandService service;

    @BeforeEach
    void setUp() {
        articles = new FakeArticleRepository();
        service = new ArticleCommandService(articles);
    }

    private Article existing(Visibility visibility) {
        return articles.save(Article.newArticle("Title", "Content", AUTHOR.id(), visibility));
    }

    @Test
    void createStoresTheArticleWithTimestamps() {
        Article created = service.create(
                new CreateArticleCommand("Title", "Content", Visibility.PUBLIC, AUTHOR));

        assertNotNull(created.id());
        assertNotNull(created.createdAt());
        assertNotNull(created.updatedAt());
        assertEquals(AUTHOR.id(), created.authorId());
    }

    @Test
    void createDefaultsToPrivateWhenVisibilityOmitted() {
        // Fail closed: an omitted flag must not publish the article.
        Article created = service.create(new CreateArticleCommand("Title", "Content", null, AUTHOR));

        assertEquals(Visibility.PRIVATE, created.visibility());
    }

    @Test
    void ownerCanUpdateTheirArticle() {
        Article article = existing(Visibility.PRIVATE);

        Article updated = service.update(new UpdateArticleCommand(
                article.id(), "New title", "New content", Visibility.PUBLIC, AUTHOR));

        assertEquals("New title", updated.title());
        assertEquals("New content", updated.content());
        assertEquals(Visibility.PUBLIC, updated.visibility());
    }

    @Test
    void updateKeepsTheOriginalAuthorAndCreatedAt() {
        // Authorship is not transferable, and an edit must not look like a new post.
        Article article = existing(Visibility.PUBLIC);

        Article updated = service.update(new UpdateArticleCommand(
                article.id(), "New title", "New content", null, AUTHOR));

        assertEquals(AUTHOR.id(), updated.authorId());
        assertEquals(article.createdAt(), updated.createdAt());
    }

    @Test
    void updateWithoutVisibilityKeepsTheCurrentOne() {
        Article article = existing(Visibility.PUBLIC);

        Article updated = service.update(new UpdateArticleCommand(
                article.id(), "New title", "New content", null, AUTHOR));

        assertEquals(Visibility.PUBLIC, updated.visibility());
    }

    @Test
    void strangerCannotUpdateAPublicArticle() {
        // Visible but not theirs: an honest 403 leaks nothing here.
        Article article = existing(Visibility.PUBLIC);

        assertThrows(ArticleAccessDeniedException.class, () -> service.update(new UpdateArticleCommand(
                article.id(), "Hijacked", "Hijacked", null, STRANGER)));
    }

    @Test
    void strangerGetsNotFoundForSomeoneElsesPrivateArticle() {
        // 403 here would confirm the article exists, so it must be 404.
        Article article = existing(Visibility.PRIVATE);

        assertThrows(ArticleNotFoundException.class, () -> service.update(new UpdateArticleCommand(
                article.id(), "Hijacked", "Hijacked", null, STRANGER)));
    }

    @Test
    void ownerCanDeleteTheirArticle() {
        Article article = existing(Visibility.PRIVATE);

        service.delete(article.id(), AUTHOR);

        assertTrue(articles.findById(article.id()).isEmpty());
    }

    @Test
    void strangerCannotDeleteAPublicArticle() {
        Article article = existing(Visibility.PUBLIC);

        assertThrows(ArticleAccessDeniedException.class, () -> service.delete(article.id(), STRANGER));
        assertTrue(articles.findById(article.id()).isPresent());
    }

    @Test
    void strangerGetsNotFoundWhenDeletingAPrivateArticle() {
        Article article = existing(Visibility.PRIVATE);

        assertThrows(ArticleNotFoundException.class, () -> service.delete(article.id(), STRANGER));
        assertTrue(articles.findById(article.id()).isPresent());
    }

    @Test
    void unknownIdIsNotFound() {
        assertThrows(ArticleNotFoundException.class, () -> service.delete(999L, AUTHOR));
        assertThrows(ArticleNotFoundException.class, () -> service.update(
                new UpdateArticleCommand(999L, "Title", "Content", null, AUTHOR)));
    }
}
