package com.jhorgi.libraryapp.adapter.in.web.dto.response;

public record TokenResponse(String token, String tokenType) {

    public static TokenResponse bearer(String token) {
        return new TokenResponse(token, "Bearer");
    }
}
