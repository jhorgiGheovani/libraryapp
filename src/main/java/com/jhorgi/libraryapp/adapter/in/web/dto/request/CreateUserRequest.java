package com.jhorgi.libraryapp.adapter.in.web.dto.request;

import com.jhorgi.libraryapp.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank
        @Size(min = 1, max = 100)
        String fullname,

        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        // Unlike /auth/register, which always yields VIEWER, an admin states the
        // role outright. Required rather than defaulted, so a privileged account
        // is never created by omission.
        @NotNull
        Role role
) {
}
