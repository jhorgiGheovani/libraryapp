package com.jhorgi.libraryapp.domain.model;

/**
 * The server-side half of an MFA challenge. Only the hash of the code is kept,
 * so a dump of the store does not hand out working codes. The role is carried
 * along so verification does not need a second trip to the database.
 */
public record OtpChallenge(String challengeId, Long userId, Role role, String otpHash) {
}
