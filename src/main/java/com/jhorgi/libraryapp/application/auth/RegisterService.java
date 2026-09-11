package com.jhorgi.libraryapp.application.auth;

import com.jhorgi.libraryapp.audit.Auditable;
import com.jhorgi.libraryapp.domain.exception.DuplicateUserException;
import com.jhorgi.libraryapp.domain.model.AuditAction;
import com.jhorgi.libraryapp.domain.model.AuditTargetType;
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

    /**
     * Unlike login and OTP verification, this one <em>can</em> use the aspect.
     * The caller is anonymous by definition — nobody is logged in yet — so there
     * is no identity for the aspect to miss, and on success the new account's id
     * comes off the returned {@link User} exactly as it does for any other create.
     *
     * <p>The target is the account, not the auth attempt, so
     * {@code ?targetType=USER&targetId=12} shows a single account's whole life:
     * registration, role changes, deletion.
     */
    @Override
    @Auditable(action = AuditAction.REGISTER, target = AuditTargetType.USER)
    public User register(String fullname, String username, String email, String password) {
        if (users.existsByUsernameOrEmail(username, email)) {
            throw new DuplicateUserException("Username or email already taken");
        }
        String hashed = passwordHasher.hash(password);
        User toSave = User.newUser(fullname, username, email, hashed, Role.VIEWER);
        return users.save(toSave);
    }
}
