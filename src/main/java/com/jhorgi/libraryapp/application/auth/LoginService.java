package com.jhorgi.libraryapp.application.auth;

import com.jhorgi.libraryapp.domain.exception.BadCredentialsException;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.port.in.LoginUseCase;
import com.jhorgi.libraryapp.domain.port.out.PasswordHasherPort;
import com.jhorgi.libraryapp.domain.port.out.TokenPort;
import com.jhorgi.libraryapp.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class LoginService implements LoginUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final TokenPort tokens;

    public LoginService(UserRepositoryPort users, PasswordHasherPort passwordHasher, TokenPort tokens) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokens = tokens;
    }

    @Override
    public String login(String email, String rawPassword) {
        User user = users.findByEmail(email)
                .orElseThrow(BadCredentialsException::new);

        if (!passwordHasher.matches(rawPassword, user.getHashedPassword())) {
            throw new BadCredentialsException();
        }

        return tokens.issue(user.getId(), user.getRole());
    }
}
