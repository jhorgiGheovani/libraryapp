package com.jhorgi.libraryapp.application.auth;

import com.jhorgi.libraryapp.domain.exception.DuplicateUserException;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.port.in.RegisterUseCase;
import com.jhorgi.libraryapp.domain.port.out.PasswordHasherPort;
import com.jhorgi.libraryapp.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class RegisterService implements RegisterUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;

    public RegisterService(UserRepositoryPort users, PasswordHasherPort passwordHasher) {
        this.users = users;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public User register(String username, String email, String password) {
        if (users.existsByUsernameOrEmail(username, email)) {
            throw new DuplicateUserException("Username or email already taken");
        }
        String hashed = passwordHasher.hash(password);
        User toSave = User.newUser(username, email, hashed, Role.VIEWER);
        return users.save(toSave);
    }
}
