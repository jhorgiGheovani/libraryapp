package com.jhorgi.libraryapp.application;

import com.jhorgi.libraryapp.application.audit.AuditQueryService;
import com.jhorgi.libraryapp.domain.exception.ForbiddenOperationException;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditEntry;
import com.jhorgi.libraryapp.domain.model.AuditOutcome;
import com.jhorgi.libraryapp.domain.model.AuditSearch;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.fake.FakeAuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditQueryServiceTest {

    private static final Actor ADMIN = new Actor(1L, Role.SUPER_ADMIN);
    private static final Instant BASE = Instant.parse("2026-09-11T10:00:00Z");

    private FakeAuditLog auditLog;
    private AuditQueryService service;

    @BeforeEach
    void setUp() {
        auditLog = new FakeAuditLog();
        service = new AuditQueryService(auditLog);
    }

    private void given(Long actorId, AuditAction action, AuditOutcome outcome, long secondsIn) {
        auditLog.save(AuditEntry.builder()
                .actorId(actorId)
                .actorRole(Role.EDITOR)
                .action(action)
                .targetType(AuditTargetType.ARTICLE)
                .outcome(outcome)
                .occurredAt(BASE.plusSeconds(secondsIn))
                .build());
    }

    // ----- the gate -----

    @Test
    void onlyAuditReadHoldersMayQueryTheTrail() {
        // Also gated by @PreAuthorize on the controller; enforced here too so the
        // rule does not depend on the web layer being in the picture.
        for (Role role : List.of(Role.EDITOR, Role.CONTRIBUTOR, Role.VIEWER)) {
            Actor actor = new Actor(7L, role);

            assertThatThrownBy(() -> service.search(AuditSearch.all(), actor, 0, 20))
                    .isInstanceOf(ForbiddenOperationException.class);
        }
    }

    @Test
    void thereIsNoReadYourOwnEntriesCarveOut() {
        // It would look harmless. It would also let someone under investigation
        // watch the investigation, and let an attacker with a session check
        // whether their own actions had been noticed.
        given(7L, AuditAction.ARTICLE_CREATE, AuditOutcome.SUCCESS, 0);
        Actor self = new Actor(7L, Role.EDITOR);

        assertThatThrownBy(() -> service.search(new AuditSearch(7L, null, null, null, null, null), self, 0, 20))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void anUnauthenticatedCallerIsRefusedRatherThanNullPointing() {
        assertThatThrownBy(() -> service.search(AuditSearch.all(), null, 0, 20))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void superAdminGetsTheTrail() {
        given(4L, AuditAction.ARTICLE_CREATE, AuditOutcome.SUCCESS, 0);

        assertThat(service.search(AuditSearch.all(), ADMIN, 0, 20).items()).hasSize(1);
    }

    // ----- filtering and paging -----

    @Test
    void filtersNarrowAndTheTotalReflectsTheFilter() {
        given(4L, AuditAction.ARTICLE_CREATE, AuditOutcome.SUCCESS, 0);
        given(4L, AuditAction.ARTICLE_DELETE, AuditOutcome.FAILURE, 1);
        given(9L, AuditAction.ARTICLE_CREATE, AuditOutcome.SUCCESS, 2);

        PagedResult<AuditEntry> mine =
                service.search(new AuditSearch(4L, null, null, null, null, null), ADMIN, 0, 20);

        assertThat(mine.items()).hasSize(2);
        // A total that ignored the filter would make paging meaningless.
        assertThat(mine.totalItems()).isEqualTo(2);
    }

    @Test
    void filtersCombineWithAnd() {
        given(4L, AuditAction.ARTICLE_CREATE, AuditOutcome.SUCCESS, 0);
        given(4L, AuditAction.ARTICLE_DELETE, AuditOutcome.FAILURE, 1);

        PagedResult<AuditEntry> denied = service.search(
                new AuditSearch(4L, AuditAction.ARTICLE_DELETE, null, AuditOutcome.FAILURE, null, null),
                ADMIN, 0, 20);

        assertThat(denied.items()).hasSize(1);
        assertThat(denied.items().get(0).action()).isEqualTo(AuditAction.ARTICLE_DELETE);
    }

    @Test
    void bothTimeBoundsAreInclusive() {
        given(4L, AuditAction.LOGIN, AuditOutcome.SUCCESS, 0);
        given(4L, AuditAction.LOGIN, AuditOutcome.SUCCESS, 60);
        given(4L, AuditAction.LOGIN, AuditOutcome.SUCCESS, 120);

        PagedResult<AuditEntry> window = service.search(
                new AuditSearch(null, null, null, null, BASE, BASE.plusSeconds(60)), ADMIN, 0, 20);

        // An exclusive bound would silently drop an entry landing exactly on it.
        assertThat(window.totalItems()).isEqualTo(2);
    }

    @Test
    void theNewestEntryComesFirst() {
        given(4L, AuditAction.LOGIN, AuditOutcome.SUCCESS, 0);
        given(4L, AuditAction.ARTICLE_CREATE, AuditOutcome.SUCCESS, 60);

        PagedResult<AuditEntry> page = service.search(AuditSearch.all(), ADMIN, 0, 20);

        assertThat(page.items().get(0).action()).isEqualTo(AuditAction.ARTICLE_CREATE);
    }

    @Test
    void aNullSearchMeansEverythingRatherThanAnError() {
        given(4L, AuditAction.LOGIN, AuditOutcome.SUCCESS, 0);

        assertThat(service.search(null, ADMIN, 0, 20).items()).hasSize(1);
    }
}
