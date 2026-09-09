package com.jhorgi.libraryapp.fake;

import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.port.out.UserRepositoryPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class FakeUserRepository implements UserRepositoryPort {

    private final Map<Long, User> byId = new HashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public Optional<User> findByUsernameOrEmail(String identifier) {
        return byId.values().stream()
                .filter(u -> u.getUsername().equals(identifier) || u.getEmail().equals(identifier))
                .findFirst();
    }

    @Override
    public boolean existsByUsernameOrEmail(String username, String email) {
        return byId.values().stream()
                .anyMatch(u -> u.getUsername().equals(username) || u.getEmail().equals(email));
    }

    @Override
    public User save(User user) {
        Long id = user.getId() != null ? user.getId() : sequence.incrementAndGet();
        User stored = new User(id, user.getFullname(), user.getUsername(), user.getEmail(),
                user.getHashedPassword(), user.getRole());
        byId.put(id, stored);
        return stored;
    }
}
