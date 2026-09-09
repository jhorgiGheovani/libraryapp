package com.jhorgi.libraryapp.fake;

import com.jhorgi.libraryapp.domain.port.out.LoginAttemptPort;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FakeLoginAttemptAdapter implements LoginAttemptPort {

    private final int maxAttempts;
    private final Map<String, Integer> failures = new HashMap<>();
    private final Set<String> locked = new HashSet<>();

    public FakeLoginAttemptAdapter(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    @Override
    public boolean isLocked(String identifier) {
        return locked.contains(identifier);
    }

    @Override
    public void recordFailure(String identifier) {
        int count = failures.merge(identifier, 1, Integer::sum);
        if (count >= maxAttempts) {
            locked.add(identifier);
            failures.remove(identifier);
        }
    }

    @Override
    public void reset(String identifier) {
        failures.remove(identifier);
        locked.remove(identifier);
    }
}
