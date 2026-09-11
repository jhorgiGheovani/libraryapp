package com.jhorgi.libraryapp.audit;

import com.jhorgi.libraryapp.domain.model.AuditEntry;
import com.jhorgi.libraryapp.domain.port.out.AuditLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * The Observer half: the only thing that writes the trail.
 *
 * <p>{@code @Async} moves the insert off the request thread, so a slow or
 * unavailable audit table costs the user nothing. Combined with the try/catch
 * it gives the property the brief asks for — audit failure never breaks the
 * request — twice over: a throw on another thread could not propagate to the
 * caller anyway, and it is swallowed here regardless so it cannot surface as an
 * uncaught-exception warning either.
 *
 * <p>Running on its own thread also means its own transaction. The entry is
 * therefore kept even when the action that produced it rolls back — which is
 * correct: an attempt that failed is exactly what the trail is for.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditLogPort auditLog;

    public AuditEventListener(AuditLogPort auditLog) {
        this.auditLog = auditLog;
    }

    @Async(AuditConfig.AUDIT_EXECUTOR)
    @EventListener
    public void on(AuditEntryRecorded event) {
        AuditEntry entry = event.entry();
        try {
            auditLog.save(entry);
        } catch (RuntimeException ex) {
            // Last resort: at least the fact survives, in the application log.
            log.error("Failed to persist audit entry action={} actorId={} target={} outcome={}",
                    entry.action(), entry.actorId(), entry.targetType(), entry.outcome(), ex);
        }
    }
}
