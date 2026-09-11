package com.jhorgi.libraryapp.application;

import com.jhorgi.libraryapp.application.auth.LoginService;
import com.jhorgi.libraryapp.domain.exception.AccountLockedException;
import com.jhorgi.libraryapp.domain.exception.BadCredentialsException;
import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditOutcome;
import com.jhorgi.libraryapp.domain.model.AuditRecord;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;
import com.jhorgi.libraryapp.domain.model.MfaChallenge;
import com.jhorgi.libraryapp.domain.model.OtpChallenge;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.fake.FakeAuditTrail;
import com.jhorgi.libraryapp.fake.FakeEmailSender;
import com.jhorgi.libraryapp.fake.FakeLoginAttemptAdapter;
import com.jhorgi.libraryapp.fake.FakeOtpGenerator;
import com.jhorgi.libraryapp.fake.FakeOtpStore;
import com.jhorgi.libraryapp.fake.FakePasswordHasher;
import com.jhorgi.libraryapp.fake.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginServiceTest {

    private static final int MAX_ATTEMPTS = 5;
    private static final long OTP_TTL_MS = 300_000L;

    private FakeUserRepository users;
    private FakePasswordHasher hasher;
    private FakeOtpStore otpStore;
    private FakeEmailSender emailSender;
    private FakeAuditTrail auditTrail;
    private LoginService service;

    @BeforeEach
    void setUp() {
        users = new FakeUserRepository();
        hasher = new FakePasswordHasher();
        otpStore = new FakeOtpStore();
        emailSender = new FakeEmailSender();
        auditTrail = new FakeAuditTrail();
        service = new LoginService(users, hasher, new FakeLoginAttemptAdapter(MAX_ATTEMPTS),
                otpStore, new FakeOtpGenerator(), emailSender, auditTrail, OTP_TTL_MS);
        users.save(User.newUser("Alice Wonderland", "alice", "alice@example.com",
                hasher.hash("password123"), Role.VIEWER));
    }

    @Test
    void returnsMfaChallengeOnValidCredentials() {
        MfaChallenge challenge = service.login("alice", "password123");

        assertNotNull(challenge.challengeId());
        assertEquals(OTP_TTL_MS / 1000, challenge.expiresInSeconds());
    }

    @Test
    void emailsTheOtpToTheAccountOwner() {
        service.login("alice", "password123");

        assertEquals(1, emailSender.sent().size());
        assertEquals("alice@example.com", emailSender.last().toEmail());
        assertEquals(FakeOtpGenerator.CODE, emailSender.last().code());
    }

    @Test
    void storesTheOtpHashedNotInClear() {
        // A dump of the store must not hand out working codes.
        MfaChallenge challenge = service.login("alice", "password123");

        OtpChallenge stored = otpStore.find(challenge.challengeId()).orElseThrow();
        assertNotEquals(FakeOtpGenerator.CODE, stored.otpHash());
        assertTrue(hasher.matches(FakeOtpGenerator.CODE, stored.otpHash()));
        assertEquals(Role.VIEWER, stored.role());
    }

    @Test
    void issuesAFreshChallengeIdEachLogin() {
        String first = service.login("alice", "password123").challengeId();
        String second = service.login("alice", "password123").challengeId();

        assertNotEquals(first, second);
    }

    @Test
    void acceptsEmailAsIdentifier() {
        assertNotNull(service.login("alice@example.com", "password123").challengeId());
    }

    @Test
    void rejectsWrongPassword() {
        assertThrows(BadCredentialsException.class,
                () -> service.login("alice", "wrong-password"));
    }

    @Test
    void sendsNoOtpWhenThePasswordIsWrong() {
        assertThrows(BadCredentialsException.class,
                () -> service.login("alice", "wrong-password"));

        assertTrue(emailSender.sent().isEmpty());
    }

    @Test
    void rejectsUnknownUser() {
        assertThrows(BadCredentialsException.class,
                () -> service.login("nobody", "password123"));
    }

    @Test
    void locksAfterMaxFailedAttempts() {
        // The user gets MAX_ATTEMPTS tries; each is plain bad credentials. The
        // last of them records the lock, which then blocks the *next* attempt.
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertThrows(BadCredentialsException.class,
                    () -> service.login("alice", "wrong-password"));
        }
        assertThrows(AccountLockedException.class,
                () -> service.login("alice", "wrong-password"));
    }

    @Test
    void locksUnknownUserToo() {
        // Lockout must not distinguish unknown users, or 423 becomes an
        // account-existence oracle.
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertThrows(BadCredentialsException.class,
                    () -> service.login("nobody", "whatever"));
        }
        assertThrows(AccountLockedException.class,
                () -> service.login("nobody", "whatever"));
    }

    @Test
    void lockOnUsernameAlsoBlocksLoginByEmail() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertThrows(BadCredentialsException.class,
                    () -> service.login("alice", "wrong-password"));
        }

        assertThrows(AccountLockedException.class,
                () -> service.login("alice@example.com", "password123"));
    }

    @Test
    void cannotForgeLockOnAnotherAccountViaKeyCollision() {
        // alice is user id 1. An attacker submitting the literal string "user:1"
        // resolves to no user, so it is namespaced as "cred:user:1" — never the
        // real "user:1" key. Locking the forged string must leave alice alone.
        for (int i = 0; i <= MAX_ATTEMPTS; i++) {
            assertThrows(RuntimeException.class,
                    () -> service.login("user:1", "whatever"));
        }
        assertNotNull(service.login("alice", "password123").challengeId());
    }

    @Test
    void correctPasswordWhileLockedStillRejected() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertThrows(RuntimeException.class,
                    () -> service.login("alice", "wrong-password"));
        }
        assertThrows(AccountLockedException.class,
                () -> service.login("alice", "password123"));
    }

    @Test
    void successResetsFailureCounter() {
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            assertThrows(BadCredentialsException.class,
                    () -> service.login("alice", "wrong-password"));
        }
        // A success before the lock trips clears the counter...
        assertNotNull(service.login("alice", "password123"));
        // ...so the next failure starts over rather than locking.
        assertThrows(BadCredentialsException.class,
                () -> service.login("alice", "wrong-password"));
    }

    // ----- audit trail (slice 6) -----

    @Test
    void aSuccessfulPasswordCheckIsRecordedAgainstTheAccount() {
        service.login("alice@example.com", "password123");

        AuditRecord record = auditTrail.last().orElseThrow();
        assertEquals(AuditAction.LOGIN, record.action());
        assertEquals(AuditTargetType.AUTH, record.targetType());
        assertEquals(AuditOutcome.SUCCESS, record.outcome());
        // Attributed to the account, not to the identifier that happened to be
        // used — logging in by email and by username is the same person.
        assertEquals(Role.VIEWER, record.actorRole());
        assertNotNull(record.actorId());
    }

    @Test
    void aWrongPasswordOnAKnownAccountIsRecordedAgainstThatAccount() {
        assertThrows(BadCredentialsException.class, () -> service.login("alice", "wrong-password"));

        AuditRecord record = auditTrail.last().orElseThrow();
        assertEquals(AuditOutcome.FAILURE, record.outcome());
        assertEquals("BadCredentials", record.detail());
        assertNotNull(record.actorId());
    }

    @Test
    void anUnknownCredentialIsNeverWrittenIntoTheTrail() {
        // People type their password into the username box. Recording the raw
        // credential would turn the audit table into a place plaintext passwords
        // live — readable by the one role that can query it.
        assertThrows(BadCredentialsException.class,
                () -> service.login("hunter2-my-actual-password", "whatever"));

        AuditRecord record = auditTrail.last().orElseThrow();
        assertEquals(AuditOutcome.FAILURE, record.outcome());
        assertNull(record.actorId());
    }

    @Test
    void aLockedOutAttemptIsRecordedSeparatelyFromABadPassword() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertThrows(BadCredentialsException.class, () -> service.login("alice", "wrong-password"));
        }
        assertThrows(AccountLockedException.class, () -> service.login("alice", "password123"));

        // Distinguishable, so an administrator can tell a forgotten password from
        // an attack that has already tripped the lock and is still going.
        assertEquals("AccountLocked", auditTrail.last().orElseThrow().detail());
    }

    @Test
    void everyAttemptLeavesExactlyOneEntry() {
        assertThrows(BadCredentialsException.class, () -> service.login("alice", "wrong-password"));
        service.login("alice", "password123");

        assertEquals(2, auditTrail.recordsOf(AuditAction.LOGIN).size());
    }
}
