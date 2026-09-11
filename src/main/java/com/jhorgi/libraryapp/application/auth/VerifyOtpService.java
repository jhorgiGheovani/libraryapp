package com.jhorgi.libraryapp.application.auth;

import com.jhorgi.libraryapp.domain.exception.InvalidOtpException;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditOutcome;
import com.jhorgi.libraryapp.domain.model.AuditRecord;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;
import com.jhorgi.libraryapp.domain.model.OtpChallenge;
import com.jhorgi.libraryapp.domain.port.in.VerifyOtpUseCase;
import com.jhorgi.libraryapp.domain.port.out.AuditTrailPort;
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
    private final AuditTrailPort auditTrail;
    private final int maxAttempts;

    public VerifyOtpService(OtpStorePort otpStore, PasswordHasherPort passwordHasher, TokenPort tokens,
                            AuditTrailPort auditTrail,
                            @Value("${security.otp.max-attempts:5}") int maxAttempts) {
        this.otpStore = otpStore;
        this.passwordHasher = passwordHasher;
        this.tokens = tokens;
        this.auditTrail = auditTrail;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Audited explicitly, for the same reason as {@code LoginService#login}: the
     * caller is identified by the challenge, not by the arguments, and an
     * unknown or expired challenge identifies nobody.
     *
     * <p>This is the event that actually issues the token, so it is the one that
     * marks the start of a session — worth having as its own action rather than
     * folded into LOGIN.
     */
    @Override
    public String verify(String challengeId, String code) {
        OtpChallenge challenge = otpStore.find(challengeId).orElse(null);
        if (challenge == null) {
            // No actor: an expired, already-used or invented challenge id.
            recordVerify(null, AuditOutcome.FAILURE, "UnknownChallenge");
            throw new InvalidOtpException();
        }

        Actor actor = new Actor(challenge.userId(), challenge.role());

        if (!passwordHasher.matches(code, challenge.otpHash())) {
            boolean burned = otpStore.recordFailedAttempt(challengeId) >= maxAttempts;
            if (burned) {
                // Burn the challenge rather than let it be guessed out for the
                // rest of its TTL; the user has to log in again.
                otpStore.delete(challengeId);
            }
            recordVerify(actor, AuditOutcome.FAILURE, burned ? "WrongCodeChallengeBurned" : "WrongCode");
            throw new InvalidOtpException();
        }

        // Single use: a code that worked once is gone.
        otpStore.delete(challengeId);
        recordVerify(actor, AuditOutcome.SUCCESS, null);
        return tokens.issue(challenge.userId(), challenge.role());
    }

    private void recordVerify(Actor actor, AuditOutcome outcome, String detail) {
        auditTrail.record(AuditRecord.of(
                actor, AuditAction.OTP_VERIFY, AuditTargetType.AUTH, outcome, detail));
    }
}
