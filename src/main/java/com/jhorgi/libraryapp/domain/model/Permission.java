package com.jhorgi.libraryapp.domain.model;

public enum Permission {

    /** Write a new article. Denied to VIEWER, so a viewer never owns drafts. */
    ARTICLE_CREATE,

    /** Edit an article you authored. */
    ARTICLE_UPDATE_OWN,

    /** Delete an article you authored. Denied to CONTRIBUTOR by the brief. */
    ARTICLE_DELETE_OWN,

    /** See other people's PRIVATE articles, in reads and in listings. */
    ARTICLE_READ_ALL,

    /** Edit or delete an article regardless of who wrote it. */
    ARTICLE_WRITE_ANY,

    /** Create, read, edit and delete user accounts, including assigning roles. */
    USER_MANAGE
}
