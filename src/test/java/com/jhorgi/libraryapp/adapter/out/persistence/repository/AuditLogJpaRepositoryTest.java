package com.jhorgi.libraryapp.adapter.out.persistence.repository;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.AuditLogEntity;
import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditOutcome;
import com.jhorgi.libraryapp.domain.model.AuditSearch;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;
import com.jhorgi.libraryapp.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what the fake cannot prove: that the six optional filters really are
 * predicates in SQL, so LIMIT and COUNT both respect them. Embedded H2 via
 * @DataJpaTest, no Docker.
 */
@DataJpaTest
class AuditLogJpaRepositoryTest {

    private static final Long ALICE = 1L;
    private static final Long BOB = 2L;
    private static final Instant BASE = Instant.parse("2026-09-11T10:00:00Z");

    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "id"));

    @Autowired
    private AuditLogJpaRepository repository;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        save(ALICE, AuditAction.LOGIN, AuditOutcome.SUCCESS, AuditTargetType.AUTH, 0);
        save(ALICE, AuditAction.ARTICLE_CREATE, AuditOutcome.SUCCESS, AuditTargetType.ARTICLE, 60);
        save(ALICE, AuditAction.ARTICLE_DELETE, AuditOutcome.FAILURE, AuditTargetType.ARTICLE, 120);
        save(BOB, AuditAction.LOGIN, AuditOutcome.FAILURE, AuditTargetType.AUTH, 180);
        save(null, AuditAction.LOGIN, AuditOutcome.FAILURE, AuditTargetType.AUTH, 240);
    }

    private void save(Long actorId, AuditAction action, AuditOutcome outcome,
                      AuditTargetType targetType, long secondsIn) {
        repository.save(new AuditLogEntity(
                null, actorId, actorId == null ? null : Role.EDITOR, action, targetType,
                outcome, null, "203.0.113.7", "Mozilla/5.0", "Chrome", "Windows", "Desktop",
                BASE.plusSeconds(secondsIn)));
    }

    private Page<AuditLogEntity> search(AuditSearch criteria, int page, int size) {
        return repository.search(criteria, PageRequest.of(page, size, NEWEST_FIRST));
    }

    @Test
    void anEmptySearchReturnsTheWholeTrail() {
        assertThat(search(AuditSearch.all(), 0, 20).getTotalElements()).isEqualTo(5);
    }

    @Test
    void eachFilterNarrowsOnItsOwn() {
        assertThat(search(new AuditSearch(ALICE, null, null, null, null, null), 0, 20)
                .getTotalElements()).isEqualTo(3);
        assertThat(search(new AuditSearch(null, AuditAction.LOGIN, null, null, null, null), 0, 20)
                .getTotalElements()).isEqualTo(3);
        assertThat(search(new AuditSearch(null, null, AuditTargetType.ARTICLE, null, null, null), 0, 20)
                .getTotalElements()).isEqualTo(2);
        assertThat(search(new AuditSearch(null, null, null, AuditOutcome.FAILURE, null, null), 0, 20)
                .getTotalElements()).isEqualTo(3);
    }

    @Test
    void filtersCombineWithAnd() {
        Page<AuditLogEntity> failedLogins = search(
                new AuditSearch(null, AuditAction.LOGIN, null, AuditOutcome.FAILURE, null, null), 0, 20);

        assertThat(failedLogins.getTotalElements()).isEqualTo(2);
        assertThat(failedLogins.getContent()).allMatch(e -> e.getAction() == AuditAction.LOGIN);
        assertThat(failedLogins.getContent()).allMatch(e -> e.getOutcome() == AuditOutcome.FAILURE);
    }

    @Test
    void bothTimeBoundsAreInclusive() {
        Page<AuditLogEntity> window = search(
                new AuditSearch(null, null, null, null, BASE, BASE.plusSeconds(120)), 0, 20);

        assertThat(window.getTotalElements()).isEqualTo(3);
    }

    @Test
    void anOpenEndedWindowIsAllowed() {
        assertThat(search(new AuditSearch(null, null, null, null, BASE.plusSeconds(120), null), 0, 20)
                .getTotalElements()).isEqualTo(3);
        assertThat(search(new AuditSearch(null, null, null, null, null, BASE.plusSeconds(60)), 0, 20)
                .getTotalElements()).isEqualTo(2);
    }

    @Test
    void theNewestEntryComesFirst() {
        Page<AuditLogEntity> all = search(AuditSearch.all(), 0, 20);

        assertThat(all.getContent().get(0).getOccurredAt()).isEqualTo(BASE.plusSeconds(240));
        assertThat(all.getContent().get(4).getOccurredAt()).isEqualTo(BASE);
    }

    @Test
    void pagingIsAppliedInSqlAndTheTotalStillCountsTheWholeMatch() {
        Page<AuditLogEntity> firstPage = search(AuditSearch.all(), 0, 2);

        assertThat(firstPage.getContent()).hasSize(2);
        // The point of filtering in SQL: LIMIT narrows the page, not the count.
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(search(AuditSearch.all(), 2, 2).getContent()).hasSize(1);
    }

    @Test
    void anAnonymousEntryIsStoredAndFoundByItsAction() {
        // A failed login by an unknown credential has no actor. It is exactly the
        // row an administrator goes looking for, so it has to be queryable.
        Page<AuditLogEntity> anonymous = search(
                new AuditSearch(null, AuditAction.LOGIN, null, AuditOutcome.FAILURE, null, null), 0, 20);

        assertThat(anonymous.getContent()).anyMatch(e -> e.getActorId() == null);
    }

    @Test
    void theCapturedDeviceColumnsSurviveARoundTrip() {
        AuditLogEntity stored = search(AuditSearch.all(), 0, 1).getContent().get(0);

        assertThat(stored.getIpAddress()).isEqualTo("203.0.113.7");
        assertThat(stored.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(stored.getBrowser()).isEqualTo("Chrome");
        assertThat(stored.getOperatingSystem()).isEqualTo("Windows");
        assertThat(stored.getDevice()).isEqualTo("Desktop");
    }
}
