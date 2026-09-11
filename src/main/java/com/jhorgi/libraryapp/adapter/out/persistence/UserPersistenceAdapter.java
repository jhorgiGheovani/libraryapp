package com.jhorgi.libraryapp.adapter.out.persistence;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.UserEntity;
import com.jhorgi.libraryapp.adapter.out.persistence.mapper.UserMapper;
import com.jhorgi.libraryapp.adapter.out.persistence.repository.UserJpaRepository;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.port.out.UserRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private static final Sort BY_ID = Sort.by(Sort.Direction.ASC, "id");

    private final UserJpaRepository jpa;

    public UserPersistenceAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String identifier) {
        return jpa.findByUsernameOrEmail(identifier, identifier).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByUsernameOrEmail(String username, String email) {
        return jpa.existsByUsernameOrEmail(username, email);
    }

    @Override
    public boolean existsByUsernameOrEmailForOtherUser(String username, String email, Long excludedId) {
        return jpa.existsByUsernameOrEmailForOtherUser(username, email, excludedId);
    }

    @Override
    public boolean existsByRole(Role role) {
        return jpa.existsByRole(role);
    }

    @Override
    public PagedResult<User> findAll(int page, int size) {
        Page<UserEntity> found = jpa.findAll(PageRequest.of(page, size, BY_ID));
        List<User> items = found.getContent().stream().map(UserMapper::toDomain).toList();
        return new PagedResult<>(items, page, size, found.getTotalElements());
    }

    @Override
    public User save(User user) {
        UserEntity saved = jpa.save(UserMapper.toEntity(user));
        return UserMapper.toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
