package com.jhorgi.libraryapp.fake;

import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.TokenPayload;
import com.jhorgi.libraryapp.domain.port.out.TokenPort;

public class FakeTokenAdapter implements TokenPort {

    @Override
    public String issue(Long userId, Role role) {
        return "token-" + userId + "-" + role.name();
    }

    @Override
    public TokenPayload verify(String token) {
        String[] parts = token.split("-");
        return new TokenPayload(Long.valueOf(parts[1]), Role.valueOf(parts[2]));
    }
}
