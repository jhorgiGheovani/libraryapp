package com.jhorgi.libraryapp.domain.exception;

/**
 * Thrown both when an article does not exist and when the caller may not see
 * it, so a private article cannot be discovered by probing ids.
 */
public class ArticleNotFoundException extends RuntimeException {

    private static final String MESSAGE = "Article not found";

    public ArticleNotFoundException() {
        super(MESSAGE);
    }
}
