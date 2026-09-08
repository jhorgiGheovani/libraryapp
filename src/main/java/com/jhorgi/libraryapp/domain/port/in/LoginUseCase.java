package com.jhorgi.libraryapp.domain.port.in;

public interface LoginUseCase {

    String login(String identifier, String rawPassword);
}
