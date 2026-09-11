package com.jhorgi.libraryapp.config;

import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.fake.FakePasswordHasher;
import com.jhorgi.libraryapp.fake.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seeder is disabled in src/test/resources/application.properties so it can
 * never write to a real database during a test run — which means its behaviour
 * has to be proven here instead, against fakes.
 */
class BootstrapAdminInitializerTest {

    private FakeUserRepository users;

    @BeforeEach
    void setUp() {
        users = new FakeUserRepository();
    }

    private BootstrapAdminInitializer initializer(String username, String email, String password) {
        return new BootstrapAdminInitializer(
                users, new FakePasswordHasher(), "Super Admin", username, email, password);
    }

    private BootstrapAdminInitializer configured() {
        return initializer("root", "root@example.com", "password123");
    }

    @Test
    void seedsTheFirstSuperAdminWhenNoneExists() {
        configured().run(null);

        User seeded = users.findByUsernameOrEmail("root").orElseThrow();
        assertEquals(Role.SUPER_ADMIN, seeded.getRole());
        assertEquals("root@example.com", seeded.getEmail());
        assertNotEquals("password123", seeded.getHashedPassword(), "the password must be hashed");
    }

    @Test
    void doesNothingWhenASuperAdminAlreadyExists() {
        users.save(User.newUser("Existing", "existing", "existing@example.com", "hashed", Role.SUPER_ADMIN));

        configured().run(null);

        assertTrue(users.findByUsernameOrEmail("root").isEmpty(),
                "an existing admin means the seeder must not fire");
    }

    @Test
    void checksForAnyAdminNotForItsOwnUsername() {
        // The difference matters: keying on the configured username would let a
        // renamed or deleted seed account be recreated on the next boot, turning
        // the properties into a permanent backdoor.
        users.save(User.newUser("Renamed", "someone-else", "else@example.com", "hashed", Role.SUPER_ADMIN));

        configured().run(null);

        assertEquals(1, users.findAll(0, 10).totalItems());
    }

    @Test
    void doesNotCountNonAdminsAsAnExistingAdmin() {
        users.save(User.newUser("Viewer", "viewer", "viewer@example.com", "hashed", Role.VIEWER));

        configured().run(null);

        assertTrue(users.findByUsernameOrEmail("root").isPresent());
    }

    @Test
    void failsLoudlyRatherThanInventingADefaultCredential() {
        // A generated or hard-coded admin password is exactly the kind of thing
        // that survives into production unnoticed, so startup stops instead.
        assertThrows(IllegalStateException.class,
                () -> initializer("", "root@example.com", "password123").run(null));
        assertThrows(IllegalStateException.class,
                () -> initializer("root", "", "password123").run(null));
        assertThrows(IllegalStateException.class,
                () -> initializer("root", "root@example.com", "  ").run(null));

        assertEquals(0, users.findAll(0, 10).totalItems());
    }

    @Test
    void blankConfigIsFineOnceAnAdminExists() {
        // The existence check runs first, so a deployment that seeded its admin
        // long ago does not have to keep the credentials around to boot.
        users.save(User.newUser("Existing", "existing", "existing@example.com", "hashed", Role.SUPER_ADMIN));

        initializer("", "", "").run(null);

        assertEquals(1, users.findAll(0, 10).totalItems());
    }
}
