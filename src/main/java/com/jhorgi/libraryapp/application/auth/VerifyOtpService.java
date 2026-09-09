package com.jhorgi.libraryapp.application.auth;

import com.jhorgi.libraryapp.domain.exception.InvalidOtpException;
import com.jhorgi.libraryapp.domain.model.OtpChallenge;
import com.jhorgi.libraryapp.domain.port.in.VerifyOtpUseCase;
import com.jhorgi.libraryapp.domain.port.out.OtpStorePort;
import com.jhorgi.libraryapp.domain.port.out.PasswordHasherPort;
import com.jhorgi.libraryapp.domain.port.out.TokenPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VerifyOtpService implements VerifyOtpUseCase {

    private final OtpStorePort otpStore;
    private final PasswordHasherPort passwordHasher;
    private final TokenPort tokens;
    private final int maxAttempts;

    public VerifyOtpService(OtpStorePort otpStore, PasswordHasherPort passwordHasher, TokenPort tokens,
                            @Value("${security.otp.max-attempts:5}") int maxAttempts) {
        this.otpStore = otpStore;
        this.passwordHasher = passwordHasher;
        this.tokens = tokens;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public String verify(String challengeId, String code) {
        OtpChallenge challenge = otpStore.find(challengeId).orElseThrow(InvalidOtpException::new);

        if (!passwordHasher.matches(code, challenge.otpHash())) {
            if (otpStore.recordFailedAttempt(challengeId) >= maxAttempts) {
                // Burn the challenge rather than let it be guessed out for the
                // rest of its TTL; the user has to log in again.
                otpStore.delete(challengeId);
            }
            throw new InvalidOtpException();
        }

        // Single use: a code that worked once is gone.
        otpStore.delete(challengeId);
        return tokens.issue(challenge.userId(), challenge.role());
    }
}
