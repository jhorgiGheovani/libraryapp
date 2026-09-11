package com.jhorgi.libraryapp.domain.model;

/**
 * Every action the trail can hold, closed as an enum rather than a free string.
 *
 * <p>A string action drifts — {@code "article.delete"} in one caller and
 * {@code "DELETE_ARTICLE"} in the next — and then the query endpoint's filter
 * silently matches nothing. Closed here, an unknown action does not compile, and
 * {@code ?action=} is validated by Jackson for free.
 */
public enum AuditAction {

    /** Self-service signup. Anonymous by definition: nobody is logged in yet. */
    REGISTER,

    /** Step 1 of login: the password check. Recorded on success and on failure. */
    LOGIN,

    /** Step 2 of login: the emailed code exchanged for a JWT. */
    OTP_VERIFY,

    ARTICLE_CREATE,
    ARTICLE_UPDATE,
    ARTICLE_DELETE,

    /**
     * One article fetched by id. Kept apart from {@link #ARTICLE_LIST} so reads
     * of a named article stay findable — a list is browsing, a by-id read is
     * someone going after a specific thing.
     */
    ARTICLE_READ,

    /** A page of articles. Carries no target id: the target is the query. */
    ARTICLE_LIST,

    USER_CREATE,
    USER_UPDATE,
    USER_READ,
    USER_LIST,

    /** Kept apart from USER_UPDATE: this is the only action that moves privileges. */
    USER_ROLE_CHANGE,

    USER_DELETE
}
