package com.jhorgi.libraryapp.application;

import com.jhorgi.libraryapp.application.article.ArticleCommandService;
import com.jhorgi.libraryapp.application.article.ArticleQueryService;
import com.jhorgi.libraryapp.domain.exception.ArticleAccessDeniedException;
import com.jhorgi.libraryapp.domain.exception.ArticleNotFoundException;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.Visibility;
import com.jhorgi.libraryapp.domain.port.in.ArticleCommandUseCase.CreateArticleCommand;
import com.jhorgi.libraryapp.domain.port.in.ArticleCommandUseCase.UpdateArticleCommand;
import com.jhorgi.libraryapp.fake.FakeArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The slice-5 role matrix, exercised through the services rather than against
 * ArticlePolicy directly — a rule that holds in the policy but is never called
 * by the service would be worthless.
 *
 * <pre>
 * SUPER_ADMIN  full CRUD on any article
 * EDITOR       CRUD own; read all, including other people's drafts
 * CONTRIBUTOR  create and update own; no delete at all
 * VIEWER       read public plus own drafts; no writes
 * </pre>
 */
class ArticleRbacMatrixTest {

    private static final Long OWNER_ID = 1L;

    private static final Actor OWNER = new Actor(OWNER_ID, Role.CONTRIBUTOR);
    private static final Actor SUPER_ADMIN = new Actor(2L, Role.SUPER_ADMIN);
    private static final Actor EDITOR = new Actor(3L, Role.EDITOR);
    private static final Actor CONTRIBUTOR = new Actor(4L, Role.CONTRIBUTOR);
    private static final Actor VIEWER = new Actor(5L, Role.VIEWER);

    private FakeArticleRepository articles;
    private ArticleCommandService commands;
    private ArticleQueryService queries;

    @BeforeEach
    void setUp() {
        articles = new FakeArticleRepository();
        commands = new ArticleCommandService(articles);
        queries = new ArticleQueryService(articles);
    }

    private Article ownedBy(Long authorId, Visibility visibility) {
        return articles.save(Article.newArticle("Title", "Content", authorId, visibility));
    }

    private void update(Article article, Actor actor) {
        commands.update(new UpdateArticleCommand(article.id(), "Edited", "Edited", null, actor));
    }

    // ----- create -----

    @Test
    void viewerCannotCreateArticles() {
        // Enforced in the service too, not only by @PreAuthorize, so the rule
        // survives any caller that bypasses the web layer.
        assertThrows(ArticleAccessDeniedException.class, () -> commands.create(
                new CreateArticleCommand("Title", "Content", Visibility.PUBLIC, VIEWER)));
    }

    @Test
    void everyWritingRoleCanCreateArticles() {
        for (Actor actor : List.of(SUPER_ADMIN, EDITOR, CONTRIBUTOR)) {
            Article created = commands.create(
                    new CreateArticleCommand("Title", "Content", null, actor));
            assertEquals(actor.id(), created.authorId());
        }
    }

    // ----- delete -----

    @Test
    void contributorCannotDeleteEvenTheirOwnArticle() {
        // The one rule that is about role alone: ownership does not rescue it.
        Article own = ownedBy(CONTRIBUTOR.id(), Visibility.PUBLIC);

        assertThrows(ArticleAccessDeniedException.class, () -> commands.delete(own.id(), CONTRIBUTOR));
        assertTrue(articles.findById(own.id()).isPresent());
    }

    @Test
    void editorCanDeleteTheirOwnButNotSomeoneElsesArticle() {
        Article own = ownedBy(EDITOR.id(), Visibility.PRIVATE);
        Article theirs = ownedBy(OWNER_ID, Visibility.PUBLIC);

        commands.delete(own.id(), EDITOR);
        assertTrue(articles.findById(own.id()).isEmpty());

        assertThrows(ArticleAccessDeniedException.class, () -> commands.delete(theirs.id(), EDITOR));
        assertTrue(articles.findById(theirs.id()).isPresent());
    }

    @Test
    void superAdminCanDeleteAnyonesPrivateArticle() {
        Article theirs = ownedBy(OWNER_ID, Visibility.PRIVATE);

        commands.delete(theirs.id(), SUPER_ADMIN);

        assertTrue(articles.findById(theirs.id()).isEmpty());
    }

    @Test
    void viewerCannotDeleteAnything() {
        Article article = ownedBy(OWNER_ID, Visibility.PUBLIC);

        assertThrows(ArticleAccessDeniedException.class, () -> commands.delete(article.id(), VIEWER));
    }

    // ----- update -----

    @Test
    void contributorCanUpdateTheirOwnArticle() {
        Article own = ownedBy(CONTRIBUTOR.id(), Visibility.PRIVATE);

        update(own, CONTRIBUTOR);

        assertEquals("Edited", articles.findById(own.id()).orElseThrow().title());
    }

    @Test
    void editorCannotUpdateSomeoneElsesArticle() {
        // The editor can *see* the draft (ARTICLE_READ_ALL), so this is an honest
        // 403 rather than the 404 a contributor would get.
        Article theirs = ownedBy(OWNER_ID, Visibility.PRIVATE);

        assertThrows(ArticleAccessDeniedException.class, () -> update(theirs, EDITOR));
    }

    @Test
    void superAdminCanUpdateAnyonesPrivateArticle() {
        Article theirs = ownedBy(OWNER_ID, Visibility.PRIVATE);

        update(theirs, SUPER_ADMIN);

        Article stored = articles.findById(theirs.id()).orElseThrow();
        assertEquals("Edited", stored.title());
        assertEquals(OWNER_ID, stored.authorId(), "an admin edit must not steal authorship");
    }

    @Test
    void viewerCannotUpdateEvenAnArticleTheyOwn() {
        // A demoted user keeps read access to their drafts but loses write access.
        Article own = ownedBy(VIEWER.id(), Visibility.PRIVATE);

        assertThrows(ArticleAccessDeniedException.class, () -> update(own, VIEWER));
    }

    // ----- read -----

    @Test
    void readAllRolesSeeOtherPeoplesDrafts() {
        Article draft = ownedBy(OWNER_ID, Visibility.PRIVATE);

        assertEquals("Title", queries.getById(draft.id(), EDITOR).title());
        assertEquals("Title", queries.getById(draft.id(), SUPER_ADMIN).title());
    }

    @Test
    void rolesWithoutReadAllGetNotFoundForOtherPeoplesDrafts() {
        Article draft = ownedBy(OWNER_ID, Visibility.PRIVATE);

        assertThrows(ArticleNotFoundException.class, () -> queries.getById(draft.id(), CONTRIBUTOR));
        assertThrows(ArticleNotFoundException.class, () -> queries.getById(draft.id(), VIEWER));
    }

    @Test
    void viewerReadsPublicArticlesAndTheirOwnDrafts() {
        Article publicArticle = ownedBy(OWNER_ID, Visibility.PUBLIC);
        Article ownDraft = ownedBy(VIEWER.id(), Visibility.PRIVATE);

        assertEquals("Title", queries.getById(publicArticle.id(), VIEWER).title());
        assertEquals("Title", queries.getById(ownDraft.id(), VIEWER).title());
    }

    // ----- list -----

    @Test
    void listForReadAllRolesIncludesEveryDraft() {
        ownedBy(OWNER_ID, Visibility.PUBLIC);
        ownedBy(OWNER_ID, Visibility.PRIVATE);
        ownedBy(CONTRIBUTOR.id(), Visibility.PRIVATE);

        for (Actor actor : List.of(EDITOR, SUPER_ADMIN)) {
            PagedResult<Article> page = queries.list(actor, 0, 10);
            assertEquals(3, page.items().size());
            assertEquals(3, page.totalItems());
        }
    }

    @Test
    void listForAViewerIsStillPublicPlusOwnDrafts() {
        ownedBy(OWNER_ID, Visibility.PUBLIC);
        ownedBy(OWNER_ID, Visibility.PRIVATE);
        ownedBy(VIEWER.id(), Visibility.PRIVATE);

        PagedResult<Article> page = queries.list(VIEWER, 0, 10);

        assertEquals(2, page.items().size());
        assertEquals(2, page.totalItems(), "the total must not betray the hidden draft");
        assertTrue(page.items().stream().noneMatch(a -> a.authorId().equals(OWNER_ID) && !a.isPublic()));
    }
}
