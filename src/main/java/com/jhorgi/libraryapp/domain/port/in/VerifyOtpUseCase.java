package com.jhorgi.libraryapp.domain.port.in;

public interface VerifyOtpUseCase {

    String verify(String challengeId, String code);
}
