package com.jhorgi.libraryapp.application;

import com.jhorgi.libraryapp.application.user.UserManagementService;
import com.jhorgi.libraryapp.domain.exception.DuplicateUserException;
import com.jhorgi.libraryapp.domain.exception.ForbiddenOperationException;
import com.jhorgi.libraryapp.domain.exception.UserNotFoundException;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.model.Visibility;
import com.jhorgi.libraryapp.domain.port.in.UserManagementUseCase.CreateUserCommand;
import com.jhorgi.libraryapp.domain.port.in.UserManagementUseCase.UpdateUserCommand;
import com.jhorgi.libraryapp.fake.FakeArticleRepository;
import com.jhorgi.libraryapp.fake.FakePasswordHasher;
import com.jhorgi.libraryapp.fake.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserManagementServiceTest {

    private static final Actor ADMIN = new Actor(100L, Role.SUPER_ADMIN);

    private FakeUserRepository users;
    private FakeArticleRepository articles;
    private UserManagementService service;

    @BeforeEach
    void setUp() {
        users = new FakeUserRepository();
        articles = new FakeArticleRepository();
        service = new UserManagementService(users, articles, new FakePasswordHasher());
    }

    private User create(String username, String email, Role role) {
        return service.create(new CreateUserCommand(
                "Full Name", username, email, "password123", role, ADMIN));
    }

    // ----- permission -----

    @Test
    void onlyUserManagePermissionHoldersMayTouchTheseEndpoints() {
        // Also gated by @PreAuthorize on the controller; enforced here too so the
        // rule does not depend on the web layer being in the picture.
        for (Role role : List.of(Role.EDITOR, Role.CONTRIBUTOR, Role.VIEWER)) {
            Actor actor = new Actor(7L, role);

            assertThrows(ForbiddenOperationException.class, () -> service.list(actor, 0, 10));
            assertThrows(ForbiddenOperationException.class, () -> service.getById(1L, actor));
            assertThrows(ForbiddenOperationException.class, () -> service.delete(1L, actor));
            assertThrows(ForbiddenOperationException.class,
                    () -> service.changeRole(1L, Role.SUPER_ADMIN, actor));
            assertThrows(ForbiddenOperationException.class, () -> service.create(new CreateUserCommand(
                    "Full Name", "someone", "someone@example.com", "password123", Role.VIEWER, actor)));
        }
    }

    @Test
    void aNonAdminCannotPromoteThemselves() {
        User victim = create("victim", "victim@example.com", Role.VIEWER);
        Actor self = new Actor(victim.getId(), Role.VIEWER);

        assertThrows(ForbiddenOperationException.class,
                () -> service.changeRole(victim.getId(), Role.SUPER_ADMIN, self));
        assertEquals(Role.VIEWER, users.findById(victim.getId()).orElseThrow().getRole());
    }

    // ----- create -----

    @Test
    void createStoresTheRequestedRoleAndHashesThePassword() {
        User created = create("editor", "editor@example.com", Role.EDITOR);

        assertEquals(Role.EDITOR, created.getRole());
        assertNotEquals("password123", created.getHashedPassword());
        assertTrue(created.getHashedPassword().contains("password123"),
                "the fake hasher wraps the raw value, so this proves hashing ran");
    }

    @Test
    void createRejectsADuplicateUsernameOrEmail() {
        create("taken", "taken@example.com", Role.VIEWER);

        assertThrows(DuplicateUserException.class,
                () -> create("taken", "other@example.com", Role.VIEWER));
        assertThrows(DuplicateUserException.class,
                () -> create("other", "taken@example.com", Role.VIEWER));
    }

    // ----- read -----

    @Test
    void getByIdReturnsTheUserAndUnknownIdIsNotFound() {
        User created = create("someone", "someone@example.com", Role.VIEWER);

        assertEquals("someone", service.getById(created.getId(), ADMIN).getUsername());
        assertThrows(UserNotFoundException.class, () -> service.getById(999L, ADMIN));
    }

    @Test
    void listIsPaged() {
        for (int i = 0; i < 5; i++) {
            create("user" + i, "user" + i + "@example.com", Role.VIEWER);
        }

        PagedResult<User> first = service.list(ADMIN, 0, 2);
        PagedResult<User> second = service.list(ADMIN, 1, 2);

        assertEquals(2, first.items().size());
        assertEquals(5, first.totalItems());
        assertEquals(3, first.totalPages());
        assertTrue(first.items().stream().map(User::getId)
                .noneMatch(id -> second.items().stream().anyMatch(u -> u.getId().equals(id))));
    }

    // ----- update -----

    @Test
    void updateChangesTheProfileButNotTheRole() {
        User created = create("editor", "editor@example.com", Role.EDITOR);

        User updated = service.updateProfile(new UpdateUserCommand(
                created.getId(), "New Name", "neweditor", "new@example.com", null, ADMIN));

        assertEquals("New Name", updated.getFullname());
        assertEquals("neweditor", updated.getUsername());
        assertEquals(Role.EDITOR, updated.getRole(), "a profile edit must not move privileges");
    }

    @Test
    void updateWithoutAPasswordKeepsTheCurrentOne() {
        User created = create("editor", "editor@example.com", Role.EDITOR);

        User updated = service.updateProfile(new UpdateUserCommand(
                created.getId(), "New Name", "editor", "editor@example.com", null, ADMIN));

        assertEquals(created.getHashedPassword(), updated.getHashedPassword());
    }

    @Test
    void updateWithAPasswordHashesTheNewOne() {
        User created = create("editor", "editor@example.com", Role.EDITOR);

        User updated = service.updateProfile(new UpdateUserCommand(
                created.getId(), "Full Name", "editor", "editor@example.com", "brandnew123", ADMIN));

        assertNotEquals(created.getHashedPassword(), updated.getHashedPassword());
        assertTrue(updated.getHashedPassword().contains("brandnew123"));
    }

    @Test
    void resubmittingAnUnchangedUsernameIsNotAConflictWithItself() {
        // The uniqueness check has to exclude the row being edited, or every
        // no-op edit would fail as a duplicate of itself.
        User created = create("editor", "editor@example.com", Role.EDITOR);

        User updated = service.updateProfile(new UpdateUserCommand(
                created.getId(), "New Name", "editor", "editor@example.com", null, ADMIN));

        assertEquals("New Name", updated.getFullname());
    }

    @Test
    void updateStillRejectsAnotherUsersUsername() {
        create("taken", "taken@example.com", Role.VIEWER);
        User other = create("editor", "editor@example.com", Role.EDITOR);

        assertThrows(DuplicateUserException.class, () -> service.updateProfile(new UpdateUserCommand(
                other.getId(), "Full Name", "taken", "editor@example.com", null, ADMIN)));
    }

    @Test
    void updatingAnUnknownUserIsNotFound() {
        assertThrows(UserNotFoundException.class, () -> service.updateProfile(new UpdateUserCommand(
                999L, "Full Name", "ghost", "ghost@example.com", null, ADMIN)));
    }

    // ----- role change -----

    @Test
    void changeRoleMovesTheUserAndKeepsEverythingElse() {
        User created = create("promoted", "promoted@example.com", Role.VIEWER);

        User updated = service.changeRole(created.getId(), Role.EDITOR, ADMIN);

        assertEquals(Role.EDITOR, updated.getRole());
        assertEquals("promoted", updated.getUsername());
        assertEquals(created.getHashedPassword(), updated.getHashedPassword());
    }

    @Test
    void anAdminCannotChangeTheirOwnRole() {
        // Otherwise the last SUPER_ADMIN could demote themselves and leave nobody
        // able to promote anyone — recoverable only by hand-editing the database.
        User admin = create("admin", "admin@example.com", Role.SUPER_ADMIN);
        Actor self = new Actor(admin.getId(), Role.SUPER_ADMIN);

        assertThrows(ForbiddenOperationException.class,
                () -> service.changeRole(admin.getId(), Role.VIEWER, self));
        assertEquals(Role.SUPER_ADMIN, users.findById(admin.getId()).orElseThrow().getRole());
    }

    @Test
    void changingTheRoleOfAnUnknownUserIsNotFound() {
        assertThrows(UserNotFoundException.class,
                () -> service.changeRole(999L, Role.EDITOR, ADMIN));
    }

    // ----- delete -----

    @Test
    void deleteRemovesTheAccountAndItsArticles() {
        User author = create("author", "author@example.com", Role.EDITOR);
        Article kept = articles.save(Article.newArticle("Someone else", "Content", 42L, Visibility.PUBLIC));
        articles.save(Article.newArticle("Theirs", "Content", author.getId(), Visibility.PUBLIC));
        articles.save(Article.newArticle("Their draft", "Content", author.getId(), Visibility.PRIVATE));

        service.delete(author.getId(), ADMIN);

        assertTrue(users.findById(author.getId()).isEmpty());
        assertTrue(articles.findVisibleTo(ADMIN, 0, 10).items().stream()
                        .noneMatch(a -> a.isOwnedBy(author.getId())),
                "the cascade must take drafts as well as public articles");
        assertTrue(articles.findById(kept.id()).isPresent(), "other authors must be untouched");
    }

    @Test
    void anAdminCannotDeleteTheirOwnAccount() {
        User admin = create("admin", "admin@example.com", Role.SUPER_ADMIN);
        Actor self = new Actor(admin.getId(), Role.SUPER_ADMIN);

        assertThrows(ForbiddenOperationException.class, () -> service.delete(admin.getId(), self));
        assertTrue(users.findById(admin.getId()).isPresent());
    }

    @Test
    void deletingAnUnknownUserIsNotFound() {
        assertThrows(UserNotFoundException.class, () -> service.delete(999L, ADMIN));
    }

    @Test
    void aFailedDeleteLeavesTheArticlesAlone() {
        // The not-found check runs before the cascade, so a bad id cannot wipe
        // articles on its way out.
        Article article = articles.save(Article.newArticle("Kept", "Content", 999L, Visibility.PUBLIC));

        assertThrows(UserNotFoundException.class, () -> service.delete(999L, ADMIN));

        assertTrue(articles.findById(article.id()).isPresent());
    }
}
