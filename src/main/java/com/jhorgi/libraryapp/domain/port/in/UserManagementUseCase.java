package com.jhorgi.libraryapp.domain.port.in;

import com.jhorgi.libraryapp.domain.model.Actor;
import com.jhorgi.libraryapp.domain.model.PagedResult;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;

/**
 * Account administration. Every method takes the calling {@link Actor} because
 * the permission check belongs with the rule, not only on the controller.
 */
public interface UserManagementUseCase {

    User create(CreateUserCommand command);

    User getById(Long userId, Actor requester);

    PagedResult<User> list(Actor requester, int page, int size);

    User updateProfile(UpdateUserCommand command);

    /** Separated from the profile edit: this is the only call that moves privileges. */
    User changeRole(Long userId, Role newRole, Actor requester);

    void delete(Long userId, Actor requester);


    record CreateUserCommand(String fullname, String username, String email, String password,
                             Role role, Actor requester) {
    }

    record UpdateUserCommand(Long userId, String fullname, String username, String email,
                             String password, Actor requester) {
    }
}
