package com.jhorgi.libraryapp.adapter.out.otp;

import com.jhorgi.libraryapp.domain.port.out.OtpGeneratorPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureRandomOtpGenerator implements OtpGeneratorPort {

    private final SecureRandom random = new SecureRandom();
    private final int length;

    public SecureRandomOtpGenerator(@Value("${security.otp.length:6}") int length) {
        this.length = length;
    }

    @Override
    public String generate() {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        // Digit by digit, so leading zeros survive and every code is the same
        // length — formatting a bounded int would drop them.
        return code.toString();
    }
}
