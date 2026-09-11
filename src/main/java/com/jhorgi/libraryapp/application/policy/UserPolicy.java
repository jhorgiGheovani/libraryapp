package com.jhorgi.libraryapp.application.policy;

import com.jhorgi.libraryapp.domain.exception.ForbiddenOperationException;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Permission;

/**
 * Account administration rules. Unlike {@link ArticlePolicy} there is no
 * ownership dimension — user management is all-or-nothing, held by SUPER_ADMIN
 * alone — so the only nuance is the pair of self-inflicted actions below.
 */
public final class UserPolicy {

    private UserPolicy() {
    }

    public static void requireCanManageUsers(Actor actor) {
        if (!actor.can(Permission.USER_MANAGE)) {
            throw new ForbiddenOperationException("You are not allowed to manage users");
        }
    }

    /**
     * An admin may not change their own role or delete their own account.
     *
     * <p>This is what stops the system from being bricked: the last SUPER_ADMIN
     * demoting or removing themselves would leave nobody able to promote anyone,
     * and the only way back would be a manual UPDATE against the database.
     * Counting admins instead would still allow the last two to lock each other
     * out one after the other, so the rule is about the *self*, not the count.
     */
    public static void requireNotSelf(Actor actor, Long targetUserId, String operation) {
        if (actor.id() != null && actor.id().equals(targetUserId)) {
            throw new ForbiddenOperationException("You cannot " + operation + " your own account");
        }
    }
}
