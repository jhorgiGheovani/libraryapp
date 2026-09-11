package com.jhorgi.libraryapp.audit;

import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose calls belong in the audit trail.
 *
 * <p>Usable on any method whose signature carries the caller — either an
 * {@link com.jhorgi.libraryapp.domain.model.Actor} argument or a command
 * implementing {@link com.jhorgi.libraryapp.domain.port.in.ActorAware}.
 * {@code AuditAspect} takes the actor, the target id and the outcome from the
 * call itself, so the annotated method contains no audit code at all.
 *
 * <p>It does <em>not</em> fit login or OTP verification: there the identity is
 * discovered inside the method, and a failed attempt has none at all. Those two
 * call {@link com.jhorgi.libraryapp.domain.port.out.AuditTrailPort} directly.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    AuditAction action();

    AuditTargetType target();
}
