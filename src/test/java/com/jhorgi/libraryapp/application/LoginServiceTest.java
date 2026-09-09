package com.jhorgi.libraryapp.application;

import com.jhorgi.libraryapp.application.auth.LoginService;
import com.jhorgi.libraryapp.domain.exception.AccountLockedException;
import com.jhorgi.libraryapp.domain.exception.BadCredentialsException;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.fake.FakeLoginAttemptAdapter;
import com.jhorgi.libraryapp.fake.FakePasswordHasher;
import com.jhorgi.libraryapp.fake.FakeTokenAdapter;
import com.jhorgi.libraryapp.fake.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginServiceTest {

    private static final int MAX_ATTEMPTS = 5;

    private FakeUserRepository users;
    private FakePasswordHasher hasher;
    private LoginService service;

    @BeforeEach
    void setUp() {
        users = new FakeUserRepository();
        hasher = new FakePasswordHasher();
        service = new LoginService(users, hasher, new FakeTokenAdapter(),
                new FakeLoginAttemptAdapter(MAX_ATTEMPTS));
        users.save(User.newUser("Alice Wonderland", "alice", "alice@example.com",
                hasher.hash("password123"), Role.VIEWER));
    }

    @Test
    void returnsTokenOnValidCredentials() {
        String token = service.login("alice", "password123");

        assertNotNull(token);
        assertEquals("token-1-VIEWER", token);
    }

    @Test
    void acceptsEmailAsIdentifier() {
        String token = service.login("alice@example.com", "password123");

        assertEquals("token-1-VIEWER", token);
    }

    @Test
    void rejectsWrongPassword() {
        assertThrows(BadCredentialsException.class,
                () -> service.login("alice", "wrong-password"));
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
        assertEquals("token-1-VIEWER", service.login("alice", "password123"));
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
}
