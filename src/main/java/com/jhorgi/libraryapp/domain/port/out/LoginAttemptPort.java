package com.jhorgi.libraryapp.domain.port.out;

public interface LoginAttemptPort {

    boolean isLocked(String identifier);

    void recordFailure(String identifier);

    void reset(String identifier);
}
