package com.jhorgi.libraryapp.domain.port.out;

import com.jhorgi.libraryapp.domain.model.OtpChallenge;

import java.time.Duration;
import java.util.Optional;

public interface OtpStorePort {

    void save(OtpChallenge challenge, Duration ttl);

    Optional<OtpChallenge> find(String challengeId);

    void delete(String challengeId);

    /**
     * Counts one wrong code against the challenge and returns the running
     * total, so a challenge cannot be brute-forced within its TTL.
     */
    long recordFailedAttempt(String challengeId);
}
