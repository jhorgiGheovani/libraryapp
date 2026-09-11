package com.jhorgi.libraryapp.fake;

import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.port.out.UserRepositoryPort;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public boolean existsByUsernameOrEmail(String username, String email) {
        return byId.values().stream()
                .anyMatch(u -> u.getUsername().equals(username) || u.getEmail().equals(email));
    }

    @Override
    public boolean existsByUsernameOrEmailForOtherUser(String username, String email, Long excludedId) {
        return byId.values().stream()
                .filter(u -> !u.hasId(excludedId))
                .anyMatch(u -> u.getUsername().equals(username) || u.getEmail().equals(email));
    }

    @Override
    public boolean existsByRole(Role role) {
        return byId.values().stream().anyMatch(u -> u.getRole() == role);
    }

    @Override
    public PagedResult<User> findAll(int page, int size) {
        List<User> sorted = byId.values().stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();

        int from = Math.min(page * size, sorted.size());
        int to = Math.min(from + size, sorted.size());
        return new PagedResult<>(sorted.subList(from, to), page, size, sorted.size());
    }

    @Override
    public User save(User user) {
        Long id = user.getId() != null ? user.getId() : sequence.incrementAndGet();
        User stored = new User(id, user.getFullname(), user.getUsername(), user.getEmail(),
                user.getHashedPassword(), user.getRole());
        byId.put(id, stored);
        return stored;
    }

    @Override
    public void deleteById(Long id) {
        byId.remove(id);
    }
}
