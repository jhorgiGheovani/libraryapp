package com.jhorgi.libraryapp.config;

import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.User;
import com.jhorgi.libraryapp.domain.port.out.PasswordHasherPort;
import com.jhorgi.libraryapp.domain.port.out.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "security.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final String fullname;
    private final String username;
    private final String email;
    private final String password;

    public BootstrapAdminInitializer(UserRepositoryPort users,
                                     PasswordHasherPort passwordHasher,
                                     @Value("${security.bootstrap.fullname:Super Admin}") String fullname,
                                     @Value("${security.bootstrap.username:}") String username,
                                     @Value("${security.bootstrap.email:}") String email,
                                     @Value("${security.bootstrap.password:}") String password) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.fullname = fullname;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByRole(Role.SUPER_ADMIN)) {
            return;
        }


        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "No SUPER_ADMIN exists and security.bootstrap.{username,email,password} are not all set. "
                            + "Set them to seed the first admin, or set security.bootstrap.enabled=false "
                            + "if you intend to create it another way.");
        }

        users.save(User.newUser(fullname, username, email, passwordHasher.hash(password), Role.SUPER_ADMIN));
        log.warn("Seeded the first SUPER_ADMIN '{}' from security.bootstrap.*. "
                + "Change this password before exposing the application.", username);
    }
}
