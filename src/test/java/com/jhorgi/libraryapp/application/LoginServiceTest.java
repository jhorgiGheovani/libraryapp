package com.jhorgi.libraryapp.application;

import com.jhorgi.libraryapp.application.auth.LoginService;
import com.jhorgi.libraryapp.domain.exception.BadCredentialsException;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.fake.FakePasswordHasher;
import com.jhorgi.libraryapp.fake.FakeTokenAdapter;
import com.jhorgi.libraryapp.fake.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginServiceTest {

    private FakeUserRepository users;
    private FakePasswordHasher hasher;
    private LoginService service;

    @BeforeEach
    void setUp() {
        users = new FakeUserRepository();
        hasher = new FakePasswordHasher();
        service = new LoginService(users, hasher, new FakeTokenAdapter());
        users.save(User.newUser("alice", "alice@example.com", hasher.hash("password123"), Role.VIEWER));
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
}
