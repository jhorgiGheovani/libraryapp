package com.jhorgi.libraryapp.adapter.in.web.dto.response;

import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;

/**
 * The brief's user fields minus the password. The hash is never mapped here, so
 * no user endpoint can leak it even by accident.
 */
public record UserResponse(
        Long id,
        String fullname,
        String username,
        String email,
        Role role
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullname(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}
