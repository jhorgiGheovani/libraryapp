package com.jhorgi.libraryapp.application.user;

import com.jhorgi.libraryapp.application.policy.UserPolicy;
import com.jhorgi.libraryapp.domain.exception.DuplicateUserException;
import com.jhorgi.libraryapp.domain.exception.UserNotFoundException;
import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.port.in.UserManagementUseCase;
import com.jhorgi.libraryapp.domain.port.out.ArticleRepositoryPort;
import com.jhorgi.libraryapp.domain.port.out.PasswordHasherPort;
import com.jhorgi.libraryapp.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService implements UserManagementUseCase {

    private final UserRepositoryPort users;
    private final ArticleRepositoryPort articles;
    private final PasswordHasherPort passwordHasher;

    public UserManagementService(UserRepositoryPort users, ArticleRepositoryPort articles,
                                 PasswordHasherPort passwordHasher) {
        this.users = users;
        this.articles = articles;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public User create(CreateUserCommand command) {
        UserPolicy.requireCanManageUsers(command.requester());

        if (users.existsByUsernameOrEmail(command.username(), command.email())) {
            throw new DuplicateUserException("Username or email already taken");
        }
        return users.save(User.newUser(
                command.fullname(), command.username(), command.email(),
                passwordHasher.hash(command.password()), command.role()));
    }

    @Override
    public User getById(Long userId, Actor requester) {
        UserPolicy.requireCanManageUsers(requester);
        return require(userId);
    }

    @Override
    public PagedResult<User> list(Actor requester, int page, int size) {
        UserPolicy.requireCanManageUsers(requester);
        return users.findAll(page, size);
    }

    @Override
    public User updateProfile(UpdateUserCommand command) {
        UserPolicy.requireCanManageUsers(command.requester());
        User existing = require(command.userId());

        // Excludes the row being edited, so resubmitting an unchanged username
        // is not a conflict with itself.
        if (users.existsByUsernameOrEmailForOtherUser(
                command.username(), command.email(), command.userId())) {
            throw new DuplicateUserException("Username or email already taken");
        }

        User updated = existing.withProfile(command.fullname(), command.username(), command.email());
        if (command.password() != null && !command.password().isBlank()) {
            updated = updated.withHashedPassword(passwordHasher.hash(command.password()));
        }
        return users.save(updated);
    }

    @Override
    public User changeRole(Long userId, Role newRole, Actor requester) {
        UserPolicy.requireCanManageUsers(requester);
        UserPolicy.requireNotSelf(requester, userId, "change the role of");

        return users.save(require(userId).withRole(newRole));
    }

    /**
     * Deleting an account takes its articles with it. Both steps are in one
     * transaction so a failure cannot leave articles pointing at an account that
     * no longer exists.
     */
    @Override
    @Transactional
    public void delete(Long userId, Actor requester) {
        UserPolicy.requireCanManageUsers(requester);
        UserPolicy.requireNotSelf(requester, userId, "delete");
        require(userId);

        articles.deleteByAuthorId(userId);
        users.deleteById(userId);
    }

    private User require(Long userId) {
        return users.findById(userId).orElseThrow(UserNotFoundException::new);
    }
}
