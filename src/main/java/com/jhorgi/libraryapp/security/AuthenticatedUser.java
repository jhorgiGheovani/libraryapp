package com.jhorgi.libraryapp.security;

import com.jhorgi.libraryapp.domain.model.Role;


public record AuthenticatedUser(Long id, Role role) {
}
