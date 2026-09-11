package com.jhorgi.libraryapp.audit;

import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditEntry;
import com.jhorgi.libraryapp.domain.model.AuditRecord;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;
import com.jhorgi.libraryapp.domain.model.DeviceInfo;
import com.jhorgi.libraryapp.domain.model.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ServletAuditTrailTest {

    private static final Instant FIXED = Instant.parse("2026-09-11T10:15:30Z");
    private static final Actor EDITOR = new Actor(4L, Role.EDITOR);
    private static final String CHROME_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/128.0.0.0 Safari/537.36";

    private final List<Object> published = new ArrayList<>();
    private final ApplicationEventPublisher publisher = published::add;
    private final ServletAuditTrail trail =
            new ServletAuditTrail(publisher, Clock.fixed(FIXED, ZoneOffset.UTC));

    @AfterEach
    void tearDown() {
        AuditContext.clear();
    }

    private static AuditRecord articleCreate() {
        return AuditRecord.success(EDITOR, AuditAction.ARTICLE_CREATE, AuditTargetType.ARTICLE);
    }

    private AuditEntry publishedEntry() {
        assertThat(published).hasSize(1);
        return ((AuditEntryRecorded) published.get(0)).entry();
    }

    @Test
    void theRecordIsEnrichedWithTheAddressAndTheParsedDevice() {
        AuditContext.set(new AuditContext.RequestInfo("203.0.113.7", CHROME_WINDOWS));

        trail.record(articleCreate());

        AuditEntry entry = publishedEntry();
        assertThat(entry.ipAddress()).isEqualTo("203.0.113.7");
        assertThat(entry.browser()).isEqualTo("Chrome");
        assertThat(entry.operatingSystem()).isEqualTo("Windows");
        assertThat(entry.device()).isEqualTo("Desktop");
    }

    @Test
    void theRawHeaderIsKeptAlongsideTheParse() {
        // The parse is a convenience for a human reading the table; the header is
        // the evidence, and a wrong guess must not destroy it.
        AuditContext.set(new AuditContext.RequestInfo("203.0.113.7", CHROME_WINDOWS));

        trail.record(articleCreate());

        assertThat(publishedEntry().userAgent()).isEqualTo(CHROME_WINDOWS);
    }

    @Test
    void theApplicationsHalfOfTheRecordIsCarriedThroughUntouched() {
        AuditContext.set(new AuditContext.RequestInfo("203.0.113.7", CHROME_WINDOWS));

        trail.record(articleCreate());

        AuditEntry entry = publishedEntry();
        assertThat(entry.actorId()).isEqualTo(EDITOR.id());
        assertThat(entry.actorRole()).isEqualTo(Role.EDITOR);
        assertThat(entry.action()).isEqualTo(AuditAction.ARTICLE_CREATE);
        assertThat(entry.targetType()).isEqualTo(AuditTargetType.ARTICLE);
    }

    @Test
    void theTimestampIsTakenWhenItHappenedNotWhenItIsWritten() {
        // The write is asynchronous, so "now" at insert time is a different and
        // less useful answer than "now" at the moment of the action.
        AuditContext.set(new AuditContext.RequestInfo("203.0.113.7", CHROME_WINDOWS));

        trail.record(articleCreate());

        assertThat(publishedEntry().occurredAt()).isEqualTo(FIXED);
    }

    @Test
    void anActionOutsideAnyRequestStillProducesAnEntry() {
        // The bootstrap seeder and any future scheduled job run on no request at
        // all. Dropping the entry because there is no IP would be the wrong trade.
        trail.record(articleCreate());

        AuditEntry entry = publishedEntry();
        assertThat(entry.ipAddress()).isNull();
        assertThat(entry.userAgent()).isNull();
        assertThat(entry.browser()).isEqualTo(DeviceInfo.UNKNOWN);
        assertThat(entry.action()).isEqualTo(AuditAction.ARTICLE_CREATE);
    }

    @Test
    void anAnonymousRecordIsAllowedThrough() {
        // A failed login has no identity. An entry with a null actor is still the
        // most valuable kind of entry there is.
        trail.record(AuditRecord.failure(null, AuditAction.LOGIN, AuditTargetType.AUTH, "BadCredentials"));

        AuditEntry entry = publishedEntry();
        assertThat(entry.actorId()).isNull();
        assertThat(entry.actorRole()).isNull();
        assertThat(entry.detail()).isEqualTo("BadCredentials");
    }

    @Test
    void aPublisherThatThrowsDoesNotBreakTheCallerMidRequest() {
        // The port's contract: recording never throws. The caller's own work has
        // already succeeded or failed on its own merits.
        ServletAuditTrail broken = new ServletAuditTrail(event -> {
            throw new IllegalStateException("event bus down");
        }, Clock.fixed(FIXED, ZoneOffset.UTC));

        assertThatCode(() -> broken.record(articleCreate())).doesNotThrowAnyException();
    }
}
