package com.jhorgi.libraryapp.audit;

import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditEntry;
import com.jhorgi.libraryapp.domain.model.AuditOutcome;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;
import com.jhorgi.libraryapp.domain.model.DeviceInfo;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.fake.FakeAuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AuditEventListenerTest {

    private FakeAuditLog auditLog;
    private AuditEventListener listener;

    @BeforeEach
    void setUp() {
        auditLog = new FakeAuditLog();
        listener = new AuditEventListener(auditLog);
    }

    private static AuditEntryRecorded event() {
        return new AuditEntryRecorded(AuditEntry.builder()
                .actorId(4L)
                .actorRole(Role.EDITOR)
                .action(AuditAction.ARTICLE_DELETE)
                .targetType(AuditTargetType.ARTICLE)
                .outcome(AuditOutcome.SUCCESS)
                .ipAddress("203.0.113.7")
                .userAgent("Mozilla/5.0")
                .device(new DeviceInfo("Chrome", "Windows", "Desktop"))
                .occurredAt(Instant.parse("2026-09-11T10:15:30Z"))
                .build());
    }

    @Test
    void theEntryIsPersistedExactlyAsItWasPublished() {
        listener.on(event());

        AuditEntry stored = auditLog.entries().get(0);
        assertThat(stored.actorId()).isEqualTo(4L);
        assertThat(stored.action()).isEqualTo(AuditAction.ARTICLE_DELETE);
        assertThat(stored.ipAddress()).isEqualTo("203.0.113.7");
        assertThat(stored.browser()).isEqualTo("Chrome");
        assertThat(stored.occurredAt()).isEqualTo(Instant.parse("2026-09-11T10:15:30Z"));
    }

    @Test
    void aDatabaseFailureIsSwallowedRatherThanThrown() {
        // The brief's requirement, and the reason this listener is not simply a
        // repository call in the aspect: audit failure never breaks the request.
        // On the async path a throw could not reach the caller anyway; catching it
        // here also stops it surfacing as an uncaught-exception warning.
        auditLog.failNextSaveWith(new IllegalStateException("connection refused"));

        assertThatCode(() -> listener.on(event())).doesNotThrowAnyException();
        assertThat(auditLog.size()).isZero();
    }

    @Test
    void eachEventProducesOneRow() {
        listener.on(event());
        listener.on(event());

        assertThat(auditLog.size()).isEqualTo(2);
    }
}
