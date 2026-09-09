package com.jhorgi.libraryapp.application;

import com.jhorgi.libraryapp.application.auth.LoginService;
import com.jhorgi.libraryapp.application.auth.VerifyOtpService;
import com.jhorgi.libraryapp.domain.exception.InvalidOtpException;
import com.jhorgi.libraryapp.domain.model.MfaChallenge;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.fake.FakeEmailSender;
import com.jhorgi.libraryapp.fake.FakeLoginAttemptAdapter;
import com.jhorgi.libraryapp.fake.FakeOtpGenerator;
import com.jhorgi.libraryapp.fake.FakeOtpStore;
import com.jhorgi.libraryapp.fake.FakePasswordHasher;
import com.jhorgi.libraryapp.fake.FakeTokenAdapter;
import com.jhorgi.libraryapp.fake.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class VerifyOtpServiceTest {

    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final String CODE = FakeOtpGenerator.CODE;
    private static final String WRONG_CODE = "000000";

    private FakeOtpStore otpStore;
    private LoginService loginService;
    private VerifyOtpService service;

    @BeforeEach
    void setUp() {
        FakeUserRepository users = new FakeUserRepository();
        FakePasswordHasher hasher = new FakePasswordHasher();
        otpStore = new FakeOtpStore();
        loginService = new LoginService(users, hasher, new FakeLoginAttemptAdapter(5),
                otpStore, new FakeOtpGenerator(), new FakeEmailSender(), 300_000L);
        service = new VerifyOtpService(otpStore, hasher, new FakeTokenAdapter(), MAX_OTP_ATTEMPTS);

        users.save(User.newUser("Alice Wonderland", "alice", "alice@example.com",
                hasher.hash("password123"), Role.VIEWER));
    }

    private String challengeId() {
        return loginService.login("alice", "password123").challengeId();
    }

    @Test
    void correctCodeReturnsToken() {
        assertEquals("token-1-VIEWER", service.verify(challengeId(), CODE));
    }

    @Test
    void rejectsWrongCode() {
        assertThrows(InvalidOtpException.class, () -> service.verify(challengeId(), WRONG_CODE));
    }

    @Test
    void rejectsUnknownChallengeId() {
        assertThrows(InvalidOtpException.class, () -> service.verify("not-a-challenge", CODE));
    }

    @Test
    void rejectsExpiredChallenge() {
        String id = challengeId();
        otpStore.expire(id);

        assertThrows(InvalidOtpException.class, () -> service.verify(id, CODE));
    }

    @Test
    void codeIsSingleUse() {
        String id = challengeId();
        service.verify(id, CODE);

        // Replaying a captured code must not mint a second token.
        assertThrows(InvalidOtpException.class, () -> service.verify(id, CODE));
    }

    @Test
    void burnsChallengeAfterTooManyWrongCodes() {
        String id = challengeId();
        for (int i = 0; i < MAX_OTP_ATTEMPTS; i++) {
            assertThrows(InvalidOtpException.class, () -> service.verify(id, WRONG_CODE));
        }

        // Even the right code no longer works: the challenge is gone, so the
        // remaining TTL cannot be spent brute-forcing it.
        assertThrows(InvalidOtpException.class, () -> service.verify(id, CODE));
        assertTrue(otpStore.find(id).isEmpty());
    }

    @Test
    void wrongCodesBelowThresholdLeaveTheChallengeUsable() {
        String id = challengeId();
        for (int i = 0; i < MAX_OTP_ATTEMPTS - 1; i++) {
            assertThrows(InvalidOtpException.class, () -> service.verify(id, WRONG_CODE));
        }

        assertEquals("token-1-VIEWER", service.verify(id, CODE));
    }

    @Test
    void oneChallengeCannotBeVerifiedWithAnothersState() {
        // Two live challenges for the same user: burning the first must not
        // affect the second.
        String first = challengeId();
        String second = challengeId();
        for (int i = 0; i < MAX_OTP_ATTEMPTS; i++) {
            assertThrows(InvalidOtpException.class, () -> service.verify(first, WRONG_CODE));
        }

        assertEquals("token-1-VIEWER", service.verify(second, CODE));
    }
}
