package com.jhorgi.libraryapp.audit;

import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.AuditOutcome;
import com.jhorgi.libraryapp.domain.model.AuditRecord;
import com.jhorgi.libraryapp.domain.port.in.AuditableCommand;
import com.jhorgi.libraryapp.domain.port.out.AuditTrailPort;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Turns {@link Auditable} into a trail entry, for both outcomes.
 *
 * <p>This is the Proxy half of the audit design: article and user CRUD gained
 * full coverage without a line of audit code moving into
 * {@code ArticleCommandService} or {@code UserManagementService}, which are
 * still unit-testable against fakes with no audit infrastructure present.
 *
 * <p>A failure is recorded and <em>then</em> rethrown unchanged. The denied
 * delete and the 404-probe for someone else's private article are the entries
 * worth having, and swallowing the exception to log it would change behaviour
 * the rest of the slices already test.
 *
 * <p>Note what the aspect does <em>not</em> do: it never inspects the return
 * value. Since the trail dropped {@code target_id} it has no reason to — an
 * entry records who did what, not which row it landed on.
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditTrailPort auditTrail;

    public AuditAspect(AuditTrailPort auditTrail) {
        this.auditTrail = auditTrail;
    }

    @Around("@annotation(com.jhorgi.libraryapp.audit.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        Auditable auditable = auditableOf(joinPoint);
        Actor actor = actorOf(joinPoint.getArgs());

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable failure) {
            auditTrail.record(AuditRecord.of(actor, auditable.action(), auditable.target(),
                    AuditOutcome.FAILURE, failure.getClass().getSimpleName()));
            throw failure;
        }

        auditTrail.record(AuditRecord.success(actor, auditable.action(), auditable.target()));
        return result;
    }

    /**
     * Reads the annotation off the implementation method, not the interface one.
     * These services sit behind proxies, so the method the join point reports can
     * be the interface declaration — which carries no {@code @Auditable}.
     */
    private static Auditable auditableOf(ProceedingJoinPoint joinPoint) {
        Method declared = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Method actual = AopUtils.getMostSpecificMethod(declared, joinPoint.getTarget().getClass());
        return AnnotatedElementUtils.findMergedAnnotation(actual, Auditable.class);
    }

    /** First {@link Actor} in the argument list, or the one a command names. */
    private static Actor actorOf(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Actor actor) {
                return actor;
            }
            if (arg instanceof AuditableCommand command) {
                return command.requester();
            }
        }
        // Registration reaches here legitimately — signup is anonymous. Null
        // rather than a throw: an audit gap must never break the call it watches.
        return null;
    }
}
