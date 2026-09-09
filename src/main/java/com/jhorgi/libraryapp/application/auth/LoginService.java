package com.jhorgi.libraryapp.application.auth;

import com.jhorgi.libraryapp.domain.exception.AccountLockedException;
import com.jhorgi.libraryapp.domain.exception.BadCredentialsException;
import com.jhorgi.libraryapp.domain.model.MfaChallenge;
import com.jhorgi.libraryapp.domain.model.OtpChallenge;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.port.in.LoginUseCase;
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
    private final Duration otpTtl;

    public LoginService(UserRepositoryPort users, PasswordHasherPort passwordHasher,
                        LoginAttemptPort loginAttempts, OtpStorePort otpStore,
                        OtpGeneratorPort otpGenerator, EmailSenderPort emailSender,
                        @Value("${security.otp.ttl-ms:300000}") long otpTtlMs) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.loginAttempts = loginAttempts;
        this.otpStore = otpStore;
        this.otpGenerator = otpGenerator;
        this.emailSender = emailSender;
        this.otpTtl = Duration.ofMillis(otpTtlMs);
    }

    @Override
    public MfaChallenge login(String credential, String rawPassword) {
        User user = users.findByUsernameOrEmail(credential).orElse(null);

        //decide key apakah pakai id atau pakai cred
        String lockoutKey = lockoutKey(user, credential);

        if (loginAttempts.isLocked(lockoutKey)) {
            throw new AccountLockedException();
        }

        if (user == null || !passwordHasher.matches(rawPassword, user.getHashedPassword())) {
            loginAttempts.recordFailure(lockoutKey);
            throw new BadCredentialsException();
        }

        loginAttempts.reset(lockoutKey);
        // trigger otp
        return issueChallenge(user);
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
