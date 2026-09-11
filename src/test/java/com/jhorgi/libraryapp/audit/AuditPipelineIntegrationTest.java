package com.jhorgi.libraryapp.audit;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.AuditLogEntity;
import com.jhorgi.libraryapp.adapter.out.persistence.repository.AuditLogJpaRepository;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Article;
import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditOutcome;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.Visibility;
import com.jhorgi.libraryapp.domain.port.in.ArticleCommandUseCase;
import com.jhorgi.libraryapp.domain.port.in.ArticleCommandUseCase.CreateArticleCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The one test that exercises the pipeline end to end inside a real context:
 * annotation to pointcut to aspect to event to async listener to H2.
 *
 * <p>The unit tests above each cover one link, and all of them would still pass
 * if Spring never applied the aspect at all — a pointcut that matches nothing is
 * invisible to a hand-built {@code AspectJProxyFactory}. This is what catches
 * that.
 */
@SpringBootTest
class AuditPipelineIntegrationTest {

    private static final Actor EDITOR = new Actor(4L, Role.EDITOR);
    private static final Actor VIEWER = new Actor(9L, Role.VIEWER);

    @Autowired
    private ArticleCommandUseCase articleCommands;

    @Autowired
    private AuditLogJpaRepository auditLogs;

    @BeforeEach
    @AfterEach
    void clearTrail() {
        auditLogs.deleteAll();
    }

    /**
     * The listener runs on the audit executor, so the row appears shortly after
     * the call returns rather than during it. Polling rather than sleeping a
     * fixed interval: fast when it works, and it fails as a timeout rather than
     * as a confusing empty result.
     */
    private List<AuditLogEntity> awaitEntries(int expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            List<AuditLogEntity> found = auditLogs.findAll();
            if (found.size() >= expected) {
                return found;
            }
            Thread.sleep(20);
        }
        return auditLogs.findAll();
    }

    @Test
    @Timeout(30)
    void springReallyAppliesTheAspectAndTheRowReachesTheDatabase() throws InterruptedException {
        articleCommands.create(new CreateArticleCommand("Title", "Content", Visibility.PUBLIC, EDITOR));

        List<AuditLogEntity> entries = awaitEntries(1);

        assertThat(entries).hasSize(1);
        AuditLogEntity entry = entries.get(0);
        assertThat(entry.getAction()).isEqualTo(AuditAction.ARTICLE_CREATE);
        assertThat(entry.getTargetType()).isEqualTo(AuditTargetType.ARTICLE);
        assertThat(entry.getActorId()).isEqualTo(EDITOR.id());
        assertThat(entry.getActorRole()).isEqualTo(Role.EDITOR);
        assertThat(entry.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(entry.getOccurredAt()).isNotNull();
    }

    @Test
    @Timeout(30)
    void aRejectedCallIsPersistedToo() throws InterruptedException {
        assertThatThrownBy(() -> articleCommands.create(
                new CreateArticleCommand("Title", "Content", Visibility.PUBLIC, VIEWER)))
                .isInstanceOf(RuntimeException.class);

        List<AuditLogEntity> entries = awaitEntries(1);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(entries.get(0).getActorId()).isEqualTo(VIEWER.id());
    }

    @Test
    @Timeout(30)
    void anActionOutsideAnyHttpRequestStillGetsAnEntryJustWithoutAnAddress() throws InterruptedException {
        // No filter ran here, so AuditContext is empty. The entry must still be
        // written — the same situation the bootstrap seeder is in.
        articleCommands.create(new CreateArticleCommand("Title", "Content", Visibility.PUBLIC, EDITOR));

        List<AuditLogEntity> entries = awaitEntries(1);

        assertThat(entries.get(0).getIpAddress()).isNull();
        assertThat(entries.get(0).getBrowser()).isEqualTo("Unknown");
    }
}
