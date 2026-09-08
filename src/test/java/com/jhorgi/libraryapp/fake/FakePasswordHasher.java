package com.jhorgi.libraryapp.fake;

import com.jhorgi.libraryapp.domain.port.out.PasswordHasherPort;

public class FakePasswordHasher implements PasswordHasherPort {

    private static final String PREFIX = "hashed:";

    @Override
    public String hash(String rawPassword) {
        return PREFIX + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        return hashedPassword.equals(hash(rawPassword));
    }
}
