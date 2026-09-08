package com.jhorgi.libraryapp.adapter.out.persistence.mapper;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.UserEntity;
import com.jhorgi.libraryapp.domain.model.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getHashedPassword(),
                entity.getRole()
        );
    }

    public static UserEntity toEntity(User user) {
        return new UserEntity(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getHashedPassword(),
                user.getRole()
        );
    }
}
