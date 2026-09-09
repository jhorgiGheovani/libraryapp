package com.jhorgi.libraryapp.domain.port.out;

import java.time.Duration;

public interface EmailSenderPort {

    void sendOtp(String toEmail, String code, Duration validFor);
}
