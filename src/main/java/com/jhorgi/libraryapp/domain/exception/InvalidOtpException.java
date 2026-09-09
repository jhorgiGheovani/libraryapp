package com.jhorgi.libraryapp.domain.exception;

/**
 * One message for every failure mode of the OTP step — wrong code, expired
 * challenge, unknown challenge id, or too many tries — so the response never
 * tells an attacker which of those it was.
 */
public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException() {
        super("Invalid or expired verification code");
    }
}
