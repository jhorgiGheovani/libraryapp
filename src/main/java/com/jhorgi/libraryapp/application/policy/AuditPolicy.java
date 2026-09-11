package com.jhorgi.libraryapp.application.policy;

import com.jhorgi.libraryapp.domain.exception.ForbiddenOperationException;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Permission;

/**
 * Reading the trail is SUPER_ADMIN only, like {@link UserPolicy} and unlike
 * {@link ArticlePolicy}: there is no ownership dimension.
 *
 * <p>Note there is no "read my own entries" carve-out. It would look harmless
 * and is not: an attacker who got a session could use it to check whether their
 * own actions had been noticed, and someone under investigation could watch the
 * investigation. The trail is written by everyone and read by nobody but the
 * administrator.
 */
public final class AuditPolicy {

    private AuditPolicy() {
    }

    public static void requireCanReadAuditLog(Actor actor) {
        if (actor == null || !actor.can(Permission.AUDIT_READ)) {
            throw new ForbiddenOperationException("You are not allowed to read the audit log");
        }
    }
}
