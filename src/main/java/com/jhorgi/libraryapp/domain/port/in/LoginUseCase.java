package com.jhorgi.libraryapp.domain.port.in;

import com.jhorgi.libraryapp.domain.model.MfaChallenge;

public interface LoginUseCase {

    MfaChallenge login(String identifier, String rawPassword);
}
