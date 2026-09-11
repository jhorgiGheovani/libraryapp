package com.jhorgi.libraryapp.domain.port.out;

import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findByUsernameOrEmail(String identifier);

    Optional<User> findById(Long id);

    boolean existsByUsernameOrEmail(String username, String email);

    /**
     * The same uniqueness check, but ignoring one account. An edit that leaves
     * the username untouched must not collide with the row being edited.
     */
    boolean existsByUsernameOrEmailForOtherUser(String username, String email, Long excludedId);

    boolean existsByRole(Role role);

    PagedResult<User> findAll(int page, int size);

    User save(User user);

    void deleteById(Long id);
}
