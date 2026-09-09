package com.jhorgi.libraryapp.application;

import com.jhorgi.libraryapp.application.auth.RegisterService;
import com.jhorgi.libraryapp.domain.exception.DuplicateUserException;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.fake.FakePasswordHasher;
import com.jhorgi.libraryapp.fake.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegisterServiceTest {

    private FakeUserRepository users;
    private RegisterService service;

    @BeforeEach
    void setUp() {
        users = new FakeUserRepository();
        service = new RegisterService(users, new FakePasswordHasher());
    }

    @Test
    void registersNewUserWithViewerRoleAndHashedPassword() {
        User user = service.register("Alice Wonderland", "alice", "alice@example.com", "password123");

        assertNotNull(user.getId());
        assertEquals("Alice Wonderland", user.getFullname());
        assertEquals("alice", user.getUsername());
        assertEquals(Role.VIEWER, user.getRole());
        assertNotEquals("password123", user.getHashedPassword());
        assertEquals("hashed:password123", user.getHashedPassword());
    }

    @Test
    void rejectsDuplicateUsernameOrEmail() {
        service.register("Alice Wonderland", "alice", "alice@example.com", "password123");

        assertThrows(DuplicateUserException.class,
                () -> service.register("Alice Two", "alice", "other@example.com", "password123"));
        assertThrows(DuplicateUserException.class,
                () -> service.register("Bob Builder", "bob", "alice@example.com", "password123"));
    }
}
