package com.jhorgi.libraryapp.domain.model;

/**
 * Who is asking, as the domain sees it: an id plus the role that came off the
 * JWT. Slice 4 passed a bare {@code requesterId} around, which left no way to
 * decide anything role-dependent at the point of decision.
 */
public record Actor(Long id, Role role) {

    public boolean can(Permission permission) {
        return role != null && role.has(permission);
    }

    public boolean owns(Article article) {
        return article.isOwnedBy(id);
    }
}
