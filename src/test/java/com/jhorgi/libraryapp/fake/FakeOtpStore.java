package com.jhorgi.libraryapp.fake;

import com.jhorgi.libraryapp.domain.model.OtpChallenge;
import com.jhorgi.libraryapp.domain.port.out.OtpStorePort;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeOtpStore implements OtpStorePort {

    private final Map<String, OtpChallenge> challenges = new HashMap<>();
    private final Map<String, Long> attempts = new HashMap<>();

    @Override
    public void save(OtpChallenge challenge, Duration ttl) {
        challenges.put(challenge.challengeId(), challenge);
    }

    @Override
    public Optional<OtpChallenge> find(String challengeId) {
        return Optional.ofNullable(challenges.get(challengeId));
    }

    @Override
    public void delete(String challengeId) {
        challenges.remove(challengeId);
        attempts.remove(challengeId);
    }

    @Override
    public long recordFailedAttempt(String challengeId) {
        return attempts.merge(challengeId, 1L, Long::sum);
    }

    /** Test hook: drop a challenge without touching its counter, as a TTL would. */
    public void expire(String challengeId) {
        challenges.remove(challengeId);
    }
}
