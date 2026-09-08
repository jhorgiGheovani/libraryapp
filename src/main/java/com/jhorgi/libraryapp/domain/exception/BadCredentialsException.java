package com.jhorgi.libraryapp.domain.exception;

public class BadCredentialsException extends RuntimeException {

    private static final String MESSAGE = "Invalid credentials";

    public BadCredentialsException() {
        super(MESSAGE);
    }
}
