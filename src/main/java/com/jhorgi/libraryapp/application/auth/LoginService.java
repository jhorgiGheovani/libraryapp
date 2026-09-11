package com.jhorgi.libraryapp.application.auth;

import com.jhorgi.libraryapp.domain.exception.AccountLockedException;
import com.jhorgi.libraryapp.domain.exception.BadCredentialsException;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditOutcome;
import com.jhorgi.libraryapp.domain.model.AuditRecord;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;
import com.jhorgi.libraryapp.domain.model.MfaChallenge;
import com.jhorgi.libraryapp.domain.model.OtpChallenge;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.port.in.LoginUseCase;
import com.jhorgi.libraryapp.domain.port.out.AuditTrailPort;
import com.jhorgi.libraryapp.domain.port.out.EmailSenderPort;
import com.jhorgi.libraryapp.domain.port.out.LoginAttemptPort;
import com.jhorgi.libraryapp.domain.port.out.OtpGeneratorPort;
import com.jhorgi.libraryapp.domain.port.out.OtpStorePort;
import com.jhorgi.libraryapp.domain.port.out.PasswordHasherPort;
import com.jhorgi.libraryapp.domain.port.out.UserRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Service
public class LoginService implements LoginUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final LoginAttemptPort loginAttempts;
    private final OtpStorePort otpStore;
    private final OtpGeneratorPort otpGenerator;
    private final EmailSenderPort emailSender;
    private final AuditTrailPort auditTrail;
    private final Duration otpTtl;

    public LoginService(UserRepositoryPort users, PasswordHasherPort passwordHasher,
                        LoginAttemptPort loginAttempts, OtpStorePort otpStore,
                        OtpGeneratorPort otpGenerator, EmailSenderPort emailSender,
                        AuditTrailPort auditTrail,
                        @Value("${security.otp.ttl-ms:300000}") long otpTtlMs) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.loginAttempts = loginAttempts;
        this.otpStore = otpStore;
        this.otpGenerator = otpGenerator;
        this.emailSender = emailSender;
        this.auditTrail = auditTrail;
        this.otpTtl = Duration.ofMillis(otpTtlMs);
    }

    /**
     * Audited here rather than by {@code @Auditable}, because the aspect reads
     * the actor off the signature and this signature has none: the identity is
     * discovered halfway down, and on a failed attempt there is no identity at
     * all. An aspect would write {@code actor_id = null} on precisely the rows
     * that matter most — the repeated failures worth noticing.
     */
    @Override
    public MfaChallenge login(String credential, String rawPassword) {
        User user = users.findByUsernameOrEmail(credential).orElse(null);

        //decide key apakah pakai id atau pakai cred
        String lockoutKey = lockoutKey(user, credential);

        if (loginAttempts.isLocked(lockoutKey)) {
            recordLogin(user, AuditOutcome.FAILURE, "AccountLocked");
            throw new AccountLockedException();
        }

        if (user == null || !passwordHasher.matches(rawPassword, user.getHashedPassword())) {
            loginAttempts.recordFailure(lockoutKey);
            recordLogin(user, AuditOutcome.FAILURE, "BadCredentials");
            throw new BadCredentialsException();
        }

        loginAttempts.reset(lockoutKey);
        recordLogin(user, AuditOutcome.SUCCESS, null);
        // trigger otp
        return issueChallenge(user);
    }

    /**
     * A resolved account is attributed via {@code actorId}; an unknown credential
     * leaves an anonymous FAILURE row. The credential itself is never stored —
     * people type passwords into the username box often enough that keeping it
     * would turn the audit table into a place plaintext passwords live.
     *
     * <p>An anonymous row is still worth writing: with the IP and the timestamp
     * it is enough to see a sweep in progress.
     */
    private void recordLogin(User user, AuditOutcome outcome, String detail) {
        Actor actor = user != null ? new Actor(user.getId(), user.getRole()) : null;
        auditTrail.record(AuditRecord.of(
                actor, AuditAction.LOGIN, AuditTargetType.AUTH, outcome, detail));
    }

    private MfaChallenge issueChallenge(User user) {
        String challengeId = UUID.randomUUID().toString();
        String code = otpGenerator.generate();

        otpStore.save(
                new OtpChallenge(challengeId, user.getId(), user.getRole(), passwordHasher.hash(code)),
                otpTtl);
        emailSender.sendOtp(user.getEmail(), code, otpTtl);

        return new MfaChallenge(challengeId, otpTtl.toSeconds());
    }

    private static String lockoutKey(User user, String credential) {
        if (user != null) {
            return "user:" + user.getId();
        }
        return "cred:" + credential.trim().toLowerCase(Locale.ROOT);
    }
}
