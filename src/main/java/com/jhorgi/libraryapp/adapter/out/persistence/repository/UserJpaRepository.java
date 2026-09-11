package com.jhorgi.libraryapp.adapter.out.persistence.repository;

import com.jhorgi.libraryapp.adapter.out.persistence.entity.UserEntity;
import com.jhorgi.libraryapp.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsernameOrEmail(String username, String email);

    boolean existsByUsernameOrEmail(String username, String email);

    boolean existsByRole(Role role);

    /**
     * Written out rather than derived on purpose. A derived name would parse as
     * {@code (id <> ? AND username = ?) OR email = ?} — the OR escapes the
     * exclusion, so an unchanged username would conflict with the very row being
     * edited.
     */
    @Query("select count(u) > 0 from UserEntity u "
            + "where u.id <> :excludedId and (u.username = :username or u.email = :email)")
    boolean existsByUsernameOrEmailForOtherUser(@Param("username") String username,
                                                @Param("email") String email,
                                                @Param("excludedId") Long excludedId);
}
