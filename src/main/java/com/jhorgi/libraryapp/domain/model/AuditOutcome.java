package com.jhorgi.libraryapp.domain.model;

/**
 * Whether the audited call completed or threw.
 *
 * <p>Failures are the half of the trail worth having: a denied delete, a refused
 * login, a viewer probing private ids. A log that only records what succeeded
 * cannot answer the question an audit log exists to answer.
 */
public enum AuditOutcome {
    SUCCESS,
    FAILURE
}
