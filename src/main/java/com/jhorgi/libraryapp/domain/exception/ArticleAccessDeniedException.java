package com.jhorgi.libraryapp.domain.exception;

/**
 * For articles the caller can already see but may not change. Honest 403 here
 * leaks nothing: the article's existence is public knowledge either way.
 */
public class ArticleAccessDeniedException extends RuntimeException {

    private static final String MESSAGE = "You are not allowed to modify this article";

    public ArticleAccessDeniedException() {
        super(MESSAGE);
    }
}
