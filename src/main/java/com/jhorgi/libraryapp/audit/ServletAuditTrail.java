package com.jhorgi.libraryapp.audit;

import com.jhorgi.libraryapp.domain.model.AuditEntry;
import com.jhorgi.libraryapp.domain.model.AuditRecord;
import com.jhorgi.libraryapp.domain.model.DeviceInfo;
import com.jhorgi.libraryapp.domain.port.out.AuditTrailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * The seam between the two halves of the design: takes what the application
 * knows ({@link AuditRecord}), adds what the transport knows, and publishes the
 * finished {@link AuditEntry}.
 *
 * <p>This is where every HTTP concept stops. Above it, services and the aspect
 * deal in actors and actions; below it, a listener deals in rows. Neither knows
 * about the other.
 *
 * <p>The timestamp is taken here rather than in the database, because "when it
 * happened" and "when the writer got round to it" are different questions once
 * the write is asynchronous.
 */
@Component
public class ServletAuditTrail implements AuditTrailPort {

    private static final Logger log = LoggerFactory.getLogger(ServletAuditTrail.class);

    private final ApplicationEventPublisher events;
    private final Clock clock;

    // Explicit, because the second constructor below means Spring can no longer
    // infer which one to use.
    @Autowired
    public ServletAuditTrail(ApplicationEventPublisher events) {
        this(events, Clock.systemUTC());
    }

    /** Injectable clock, so the "timestamp is when it happened" test can pin it. */
    ServletAuditTrail(ApplicationEventPublisher events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void record(AuditRecord record) {
        try {
            AuditContext.RequestInfo request = AuditContext.current();
            DeviceInfo device = UserAgentParser.parse(request.userAgent());

            events.publishEvent(new AuditEntryRecorded(AuditEntry.builder()
                    .from(record)
                    .ipAddress(request.ipAddress())
                    .userAgent(request.userAgent())
                    .device(device)
                    .occurredAt(Instant.now(clock))
                    .build()));
        } catch (RuntimeException ex) {
            // The port's contract is that it never throws. A caller is mid-request
            // and its own work has already succeeded or failed on its own merits;
            // losing the trail entry must not change that outcome.
            log.error("Failed to publish audit entry for action {}", record.action(), ex);
        }
    }
}
