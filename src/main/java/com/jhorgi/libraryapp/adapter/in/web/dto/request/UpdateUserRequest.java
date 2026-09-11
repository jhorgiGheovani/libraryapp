package com.jhorgi.libraryapp.adapter.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Profile edit. There is deliberately no {@code role} field: a role change is a
 * privilege move and gets its own endpoint, so it cannot ride along inside an
 * otherwise routine update.
 */
public record UpdateUserRequest(

        @NotBlank
        @Size(min = 1, max = 100)
        String fullname,

        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        @NotBlank
        @Email
        String email,


        @Size(min = 8, max = 100)
        String password
) {
}
