package com.jhorgi.libraryapp.domain.port.out;

import com.jhorgi.libraryapp.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findByUsernameOrEmail(String identifier);

    boolean existsByUsernameOrEmail(String username, String email);

    User save(User user);
}
