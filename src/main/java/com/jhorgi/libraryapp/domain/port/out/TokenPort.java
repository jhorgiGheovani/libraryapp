package com.jhorgi.libraryapp.domain.port.out;

import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.TokenPayload;

public interface TokenPort {

    String issue(Long userId, Role role);

    TokenPayload verify(String token);
}
