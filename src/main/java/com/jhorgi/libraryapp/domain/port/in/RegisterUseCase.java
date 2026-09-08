package com.jhorgi.libraryapp.domain.port.in;

import com.jhorgi.libraryapp.domain.model.User;

public interface RegisterUseCase {

    User register(String username, String email, String password);
}
