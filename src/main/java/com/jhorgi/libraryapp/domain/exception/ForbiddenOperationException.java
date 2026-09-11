package com.jhorgi.libraryapp.domain.exception;

/**
 * The caller is authenticated and the target exists, but the operation itself is
 * refused — either the role does not carry it, or it is a self-inflicted action
 * the system will not perform (an admin demoting or deleting themselves).
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
