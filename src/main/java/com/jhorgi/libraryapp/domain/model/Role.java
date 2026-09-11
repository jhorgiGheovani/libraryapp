package com.jhorgi.libraryapp.domain.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The RBAC matrix, in one place.
 *
 * <p>VIEWER holds no permissions at all: it may read public articles plus its
 * own drafts, and reading is not permission-gated — it is decided per article
 * by visibility and ownership.
 */
public enum Role {

    SUPER_ADMIN(EnumSet.allOf(Permission.class)),

    EDITOR(EnumSet.of(
            Permission.ARTICLE_CREATE,
            Permission.ARTICLE_UPDATE_OWN,
            Permission.ARTICLE_DELETE_OWN,
            Permission.ARTICLE_READ_ALL)),

    CONTRIBUTOR(EnumSet.of(
            Permission.ARTICLE_CREATE,
            Permission.ARTICLE_UPDATE_OWN)),

    VIEWER(EnumSet.noneOf(Permission.class));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public Set<Permission> permissions() {
        return permissions;
    }

    public boolean has(Permission permission) {
        return permissions.contains(permission);
    }
}
