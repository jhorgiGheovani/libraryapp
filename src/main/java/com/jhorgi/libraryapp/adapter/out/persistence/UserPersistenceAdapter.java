package com.jhorgi.libraryapp.adapter.out.persistence;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.UserEntity;
import com.jhorgi.libraryapp.adapter.out.persistence.mapper.UserMapper;
import com.jhorgi.libraryapp.adapter.out.persistence.repository.UserJpaRepository;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository jpa;

    public UserPersistenceAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByEmail(String identifier) {
        return jpa.findByEmail(identifier, identifier).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String username, String email) {
        return jpa.existsByUsernameOrEmail(username, email);
    }

    @Override
    public User save(User user) {
        UserEntity saved = jpa.save(UserMapper.toEntity(user));
        return UserMapper.toDomain(saved);
    }
}
