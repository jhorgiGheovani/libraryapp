package com.jhorgi.libraryapp.application.auth;

import com.jhorgi.libraryapp.domain.exception.AccountLockedException;
import com.jhorgi.libraryapp.domain.exception.BadCredentialsException;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.port.in.LoginUseCase;
import com.jhorgi.libraryapp.domain.port.out.LoginAttemptPort;
import com.jhorgi.libraryapp.domain.port.out.PasswordHasherPort;
import com.jhorgi.libraryapp.domain.port.out.TokenPort;
import com.jhorgi.libraryapp.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class LoginService implements LoginUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final TokenPort tokens;
    private final LoginAttemptPort loginAttempts;

    public LoginService(UserRepositoryPort users, PasswordHasherPort passwordHasher, TokenPort tokens,
                        LoginAttemptPort loginAttempts) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokens = tokens;
        this.loginAttempts = loginAttempts;
    }

    @Override
    public String login(String credential, String rawPassword) {
        User user = users.findByUsernameOrEmail(credential).orElse(null);

        //decide key apakah pakai id atau pakai cred
        String lockoutKey = lockoutKey(user, credential);

        if (loginAttempts.isLocked(lockoutKey)) {
            throw new AccountLockedException();
        }

        if (user == null || !passwordHasher.matches(rawPassword, user.getHashedPassword())) {
            loginAttempts.recordFailure(lockoutKey);
            throw new BadCredentialsException();
        }

        loginAttempts.reset(lockoutKey);
        return tokens.issue(user.getId(), user.getRole());
    }

    private static String lockoutKey(User user, String credential) {
        if (user != null) {
            return "user:" + user.getId();
        }
        return "cred:" + credential.trim().toLowerCase(Locale.ROOT);
    }
}
