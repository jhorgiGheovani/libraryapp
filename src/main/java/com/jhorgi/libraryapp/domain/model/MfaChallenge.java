package com.jhorgi.libraryapp.domain.model;

/**
 * What a successful password check now returns: a handle to the pending OTP
 * step, never a token. The token is only issued once the OTP is verified.
 */
public record MfaChallenge(String challengeId, long expiresInSeconds) {
}
