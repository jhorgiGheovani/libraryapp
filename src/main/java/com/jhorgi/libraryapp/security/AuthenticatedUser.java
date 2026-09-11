package com.jhorgi.libraryapp.security;

import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.Role;


public record AuthenticatedUser(Long id, Role role) {

    /** The same caller, as the domain sees it. Keeps Spring types out of the core. */
    public Actor actor() {
        return new Actor(id, role);
    }
}
