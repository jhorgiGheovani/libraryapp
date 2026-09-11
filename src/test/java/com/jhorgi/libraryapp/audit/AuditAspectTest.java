package com.jhorgi.libraryapp.audit;

import com.jhorgi.libraryapp.application.article.ArticleCommandService;
import com.jhorgi.libraryapp.application.article.ArticleQueryService;
import com.jhorgi.libraryapp.application.auth.RegisterService;
import com.jhorgi.libraryapp.application.user.UserManagementService;
import com.jhorgi.libraryapp.domain.exception.ArticleAccessDeniedException;
import com.jhorgi.libraryapp.domain.exception.ArticleNotFoundException;
import com.jhorgi.libraryapp.domain.exception.DuplicateUserException;
import com.jhorgi.libraryapp.domain.exception.ForbiddenOperationException;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditOutcome;
import com.jhorgi.libraryapp.domain.model.AuditRecord;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.model.Visibility;
import com.jhorgi.libraryapp.domain.port.in.ArticleCommandUseCase;
import com.jhorgi.libraryapp.domain.port.in.ArticleCommandUseCase.CreateArticleCommand;
import com.jhorgi.libraryapp.domain.port.in.ArticleCommandUseCase.UpdateArticleCommand;
import com.jhorgi.libraryapp.domain.port.in.ArticleQueryUseCase;
import com.jhorgi.libraryapp.domain.port.in.RegisterUseCase;
import com.jhorgi.libraryapp.domain.port.in.UserManagementUseCase;
import com.jhorgi.libraryapp.domain.port.in.UserManagementUseCase.CreateUserCommand;
import com.jhorgi.libraryapp.fake.FakeArticleRepository;
import com.jhorgi.libraryapp.fake.FakeAuditTrail;
import com.jhorgi.libraryapp.fake.FakePasswordHasher;
import com.jhorgi.libraryapp.fake.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Drives the aspect the way Spring does — through a real proxy around the real
 * services — rather than mocking a {@code ProceedingJoinPoint}. A mocked join
 * point would happily pass while the pointcut matched nothing, which is the
 * failure mode that actually happens.
 */
class AuditAspectTest {

    // Deliberately far from any generated id: an admin may not change their own
    // role, and a collision would fail the test for the wrong reason.
    private static final Actor ADMIN = new Actor(100L, Role.SUPER_ADMIN);
    private static final Actor EDITOR = new Actor(2L, Role.EDITOR);
    private static final Actor VIEWER = new Actor(3L, Role.VIEWER);

    private FakeAuditTrail auditTrail;
    private FakeArticleRepository articles;
    private ArticleCommandUseCase articleCommands;
    private ArticleQueryUseCase articleQueries;
    private UserManagementUseCase userManagement;
    private RegisterUseCase registration;

    @BeforeEach
    void setUp() {
        auditTrail = new FakeAuditTrail();
        articles = new FakeArticleRepository();
        FakeUserRepository users = new FakeUserRepository();
        FakePasswordHasher hasher = new FakePasswordHasher();

        articleCommands = proxy(new ArticleCommandService(articles), ArticleCommandUseCase.class);
        articleQueries = proxy(new ArticleQueryService(articles), ArticleQueryUseCase.class);
        userManagement = proxy(
                new UserManagementService(users, articles, hasher), UserManagementUseCase.class);
        registration = proxy(new RegisterService(users, hasher), RegisterUseCase.class);
    }

    private User register(String username, String email) {
        return registration.register("New Person", username, email, "password123");
    }

    private <T> T proxy(Object target, Class<T> as) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new AuditAspect(auditTrail));
        return factory.getProxy(as.getClassLoader());
    }

    private Article publishedBy(Actor author) {
        return articleCommands.create(
                new CreateArticleCommand("Title", "Content", Visibility.PUBLIC, author));
    }

    // ----- the happy path -----

    @Test
    void aSuccessfulCreateIsRecorded() {
        publishedBy(EDITOR);

        AuditRecord record = auditTrail.last().orElseThrow();
        assertThat(record.action()).isEqualTo(AuditAction.ARTICLE_CREATE);
        assertThat(record.targetType()).isEqualTo(AuditTargetType.ARTICLE);
        assertThat(record.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(record.actorId()).isEqualTo(EDITOR.id());
        assertThat(record.actorRole()).isEqualTo(Role.EDITOR);
    }

    @Test
    void theActorIsFoundOnACommandObjectAsWellAsInABareArgument() {
        Article article = publishedBy(EDITOR);
        auditTrail.clear();

        // delete(Long, Actor) — bare argument.
        articleCommands.delete(article.id(), EDITOR);

        assertThat(auditTrail.last().orElseThrow().actorId()).isEqualTo(EDITOR.id());
        assertThat(auditTrail.last().orElseThrow().action()).isEqualTo(AuditAction.ARTICLE_DELETE);
    }

    @Test
    void aRoleChangeIsItsOwnActionNotAGenericUpdate() {
        User target = userManagement.create(new CreateUserCommand(
                "Bob Builder", "bob", "bob@example.com", "password123", Role.VIEWER, ADMIN));
        auditTrail.clear();

        userManagement.changeRole(target.getId(), Role.EDITOR, ADMIN);

        AuditRecord record = auditTrail.last().orElseThrow();
        assertThat(record.action()).isEqualTo(AuditAction.USER_ROLE_CHANGE);
        assertThat(record.targetType()).isEqualTo(AuditTargetType.USER);
    }

    // ----- failures, which are the point -----

    @Test
    void aDeniedDeleteIsRecordedAndTheExceptionStillReachesTheCaller() {
        Article someoneElses = publishedBy(EDITOR);
        auditTrail.clear();

        assertThatThrownBy(() -> articleCommands.delete(someoneElses.id(), VIEWER))
                .isInstanceOf(ArticleAccessDeniedException.class);

        AuditRecord record = auditTrail.last().orElseThrow();
        assertThat(record.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(record.actorId()).isEqualTo(VIEWER.id());
        assertThat(record.detail()).isEqualTo("ArticleAccessDeniedException");
    }

    @Test
    void aRejectedUpdateIsRecordedAgainstTheCallerWhoAttemptedIt() {
        // Which article was attacked is no longer recorded — the trail dropped
        // target_id. Who tried, from where, and that it failed still are.
        Article someoneElses = publishedBy(EDITOR);
        auditTrail.clear();

        assertThatThrownBy(() -> articleCommands.update(new UpdateArticleCommand(
                someoneElses.id(), "Hijacked", "...", Visibility.PUBLIC, VIEWER)))
                .isInstanceOf(ArticleAccessDeniedException.class);

        AuditRecord record = auditTrail.last().orElseThrow();
        assertThat(record.action()).isEqualTo(AuditAction.ARTICLE_UPDATE);
        assertThat(record.outcome()).isEqualTo(AuditOutcome.FAILURE);
        // The actor comes off the command, not off a bare argument.
        assertThat(record.actorId()).isEqualTo(VIEWER.id());
    }

    @Test
    void probingForAPrivateArticleLeavesATrace() {
        // The caller gets a 404 that reveals nothing. The trail records the probe,
        // which is the whole reason a 404-for-private rule needs auditing at all.
        Article privateDraft = articleCommands.create(
                new CreateArticleCommand("Draft", "Secret", Visibility.PRIVATE, EDITOR));
        auditTrail.clear();

        assertThatThrownBy(() -> articleCommands.delete(privateDraft.id(), VIEWER))
                .isInstanceOf(ArticleNotFoundException.class);

        AuditRecord record = auditTrail.last().orElseThrow();
        assertThat(record.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(record.detail()).isEqualTo("ArticleNotFoundException");
        assertThat(record.actorId()).isEqualTo(VIEWER.id());
    }

    @Test
    void aRefusedUserCreateIsRecordedWithNoTargetBecauseNoIdWasEverAssigned() {
        assertThatThrownBy(() -> userManagement.create(new CreateUserCommand(
                "Mallory", "mallory", "mallory@example.com", "password123", Role.SUPER_ADMIN, VIEWER)))
                .isInstanceOf(ForbiddenOperationException.class);

        AuditRecord record = auditTrail.last().orElseThrow();
        assertThat(record.action()).isEqualTo(AuditAction.USER_CREATE);
        assertThat(record.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(record.actorId()).isEqualTo(VIEWER.id());
    }

    // ----- scope -----

    @Test
    void exactlyOneEntryIsWrittenPerCall() {
        publishedBy(EDITOR);

        assertThat(auditTrail.size()).isEqualTo(1);
    }

    @Test
    void unannotatedMethodsAreNotAudited() {
        // The repository port is below the audited layer. Only use cases are
        // annotated, so an entry means someone asked, not that a query ran.
        articles.findVisibleTo(EDITOR, 0, 10);

        assertThat(auditTrail.records()).isEmpty();
    }

    // ----- reads -----

    @Test
    void readingOneArticleIsRecordedAgainstThatArticle() {
        Article article = publishedBy(EDITOR);
        auditTrail.clear();

        articleQueries.getById(article.id(), VIEWER);

        AuditRecord record = auditTrail.last().orElseThrow();
        assertThat(record.action()).isEqualTo(AuditAction.ARTICLE_READ);
        assertThat(record.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(record.actorId()).isEqualTo(VIEWER.id());
    }

    @Test
    void probingAPrivateArticleByIdLeavesATraceEvenThoughTheCallerSeesOnlyA404() {
        // The whole reason to audit reads. One 404 is noise; a run of them against
        // consecutive ids is enumeration, and only this table can show that.
        Article privateDraft = articleCommands.create(
                new CreateArticleCommand("Draft", "Secret", Visibility.PRIVATE, EDITOR));
        auditTrail.clear();

        assertThatThrownBy(() -> articleQueries.getById(privateDraft.id(), VIEWER))
                .isInstanceOf(ArticleNotFoundException.class);

        AuditRecord record = auditTrail.last().orElseThrow();
        assertThat(record.action()).isEqualTo(AuditAction.ARTICLE_READ);
        assertThat(record.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(record.detail()).isEqualTo("ArticleNotFoundException");
    }

    @Test
    void listingArticlesIsItsOwnAction() {
        articleQueries.list(VIEWER, 0, 10);

        AuditRecord record = auditTrail.last().orElseThrow();
        assertThat(record.action()).isEqualTo(AuditAction.ARTICLE_LIST);
        assertThat(record.targetType()).isEqualTo(AuditTargetType.ARTICLE);
        assertThat(record.actorId()).isEqualTo(VIEWER.id());
    }

    @Test
    void listingAndReadingUsersAreSeparateActions() {
        User target = userManagement.create(new CreateUserCommand(
                "Bob Builder", "bob", "bob@example.com", "password123", Role.VIEWER, ADMIN));
        auditTrail.clear();

        userManagement.list(ADMIN, 0, 10);
        userManagement.getById(target.getId(), ADMIN);

        assertThat(auditTrail.recordsOf(AuditAction.USER_LIST)).hasSize(1);
        assertThat(auditTrail.recordsOf(AuditAction.USER_READ)).hasSize(1);
    }

    @Test
    void aRefusedUserListIsRecordedToo() {
        assertThatThrownBy(() -> userManagement.list(VIEWER, 0, 10))
                .isInstanceOf(ForbiddenOperationException.class);

        AuditRecord record = auditTrail.last().orElseThrow();
        assertThat(record.action()).isEqualTo(AuditAction.USER_LIST);
        assertThat(record.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(record.actorId()).isEqualTo(VIEWER.id());
    }

    // ----- registration -----

    @Test
    void aSuccessfulRegistrationIsRecordedAnonymouslyAgainstTheNewAccount() {
        User created = register("newbie", "newbie@example.com");

        AuditRecord record = auditTrail.last().orElseThrow();
        assertThat(record.action()).isEqualTo(AuditAction.REGISTER);
        assertThat(record.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        // Nobody is logged in during signup, so there is no actor to attribute...
        assertThat(record.actorId()).isNull();
        assertThat(record.actorRole()).isNull();
        // ...but the kind of thing it acted on is still recorded.
        assertThat(record.targetType()).isEqualTo(AuditTargetType.USER);
    }

    @Test
    void aDuplicateRegistrationIsRecordedAsAFailureWithNoTarget() {
        register("taken", "taken@example.com");
        auditTrail.clear();

        assertThatThrownBy(() -> register("taken", "taken@example.com"))
                .isInstanceOf(DuplicateUserException.class);

        AuditRecord record = auditTrail.last().orElseThrow();
        assertThat(record.action()).isEqualTo(AuditAction.REGISTER);
        assertThat(record.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(record.detail()).isEqualTo("DuplicateUserException");
    }
}
