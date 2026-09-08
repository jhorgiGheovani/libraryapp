package com.jhorgi.libraryapp.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank
        String identifier,

        @NotBlank
        String password
) {
}
