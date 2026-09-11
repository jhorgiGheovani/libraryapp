package com.jhorgi.libraryapp.domain.model;

/**
 * What kind of thing the action was aimed at — the resource, not the row. The
 * trail does not record <em>which</em> article or user; {@code target_id} was
 * dropped deliberately, so this narrows a query to a resource and no further.
 */
public enum AuditTargetType {
    ARTICLE,
    USER,

    /** Login and OTP verification: the target is the session being opened, not a row. */
    AUTH
}
