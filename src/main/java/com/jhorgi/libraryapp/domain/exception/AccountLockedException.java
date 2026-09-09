package com.jhorgi.libraryapp.domain.exception;

public class AccountLockedException extends RuntimeException {

    private static final String MESSAGE = "Account temporarily locked due to too many failed login attempts";

    public AccountLockedException() {
        super(MESSAGE);
    }
}
