package com.jhorgi.libraryapp.adapter.out.persistence.repository;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.UserEntity;
import com.jhorgi.libraryapp.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for the derived query that the fake can't cover: the real
 * JPA layer must actually match by username OR email. Runs on the embedded H2
 * that @DataJpaTest auto-configures (no Docker, no Postgres).
 *
 * Scope note: this pins wiring/derivation, not Postgres-specific case/collation
 * behaviour — H2 and Postgres differ there, so no case-insensitivity is asserted.
 */
@DataJpaTest
class UserJpaRepositoryTest {

    @Autowired
    private UserJpaRepository repository;

    private UserEntity alice;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        alice = repository.save(new UserEntity(null, "Alice Wonderland", "alice",
                "alice@example.com", "hashed", Role.VIEWER));
    }

    @Test
    void findsUserByUsername() {
        // adapter always passes the identifier as both args
        Optional<UserEntity> found = repository.findByUsernameOrEmail("alice", "alice");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void findsUserByEmail() {
        Optional<UserEntity> found =
                repository.findByUsernameOrEmail("alice@example.com", "alice@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("alice");
    }

    @Test
    void returnsEmptyForUnknownIdentifier() {
        assertThat(repository.findByUsernameOrEmail("nobody", "nobody")).isEmpty();
    }

    @Test
    void existsWhenUsernameTaken() {
        assertThat(repository.existsByUsernameOrEmail("alice", "free@example.com")).isTrue();
    }

    @Test
    void existsWhenEmailTaken() {
        assertThat(repository.existsByUsernameOrEmail("free", "alice@example.com")).isTrue();
    }

    @Test
    void doesNotExistWhenNeitherMatches() {
        assertThat(repository.existsByUsernameOrEmail("free", "free@example.com")).isFalse();
    }

    @Test
    void aUserDoesNotConflictWithThemselves() {
        // The reason this query is written out instead of derived: a derived name
        // would parse as (id <> ? AND username = ?) OR email = ?, so Alice's own
        // email would match and every no-op profile edit would 409.
        assertThat(repository.existsByUsernameOrEmailForOtherUser(
                "alice", "alice@example.com", alice.getId())).isFalse();
    }

    @Test
    void anotherUsersUsernameOrEmailStillConflicts() {
        UserEntity bob = repository.save(new UserEntity(null, "Bob Builder", "bob",
                "bob@example.com", "hashed", Role.VIEWER));

        assertThat(repository.existsByUsernameOrEmailForOtherUser(
                "alice", "bob@example.com", bob.getId())).isTrue();
        assertThat(repository.existsByUsernameOrEmailForOtherUser(
                "bob", "alice@example.com", bob.getId())).isTrue();
        assertThat(repository.existsByUsernameOrEmailForOtherUser(
                "free", "free@example.com", bob.getId())).isFalse();
    }

    @Test
    void existsByRoleBacksTheBootstrapAdminCheck() {
        assertThat(repository.existsByRole(Role.SUPER_ADMIN)).isFalse();

        repository.save(new UserEntity(null, "Super Admin", "root",
                "root@example.com", "hashed", Role.SUPER_ADMIN));

        assertThat(repository.existsByRole(Role.SUPER_ADMIN)).isTrue();
    }
}
