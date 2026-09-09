package com.jhorgi.libraryapp.adapter.out.redis;

import com.jhorgi.libraryapp.domain.model.OtpChallenge;
import com.jhorgi.libraryapp.domain.model.Role;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Covers what the fake store cannot: the real hash round-trip, the TTL that
 * makes an unverified challenge disappear on its own, and the attempt counter
 * inheriting the challenge's remaining lifetime.
 *
 * Embedded, in-process Redis on a random free port — no Docker.
 */
class RedisOtpStoreAdapterTest {

    private static final String CHALLENGE_ID = "11111111-2222-3333-4444-555555555555";
    private static final String CHALLENGE_KEY = "otp:challenge:" + CHALLENGE_ID;
    private static final String ATTEMPTS_KEY = "otp:attempts:" + CHALLENGE_ID;
    private static final Duration TTL = Duration.ofMinutes(5);

    private static RedisServer server;
    private static int port;
    private static LettuceConnectionFactory factory;

    private StringRedisTemplate template;
    private RedisOtpStoreAdapter adapter;

    @BeforeAll
    static void startRedis() throws IOException {
        port = freePort();
        server = new RedisServer(port);
        server.start();
        factory = new LettuceConnectionFactory("localhost", port);
        factory.afterPropertiesSet();
    }

    @AfterAll
    static void stopRedis() throws IOException {
        if (factory != null) {
            factory.destroy();
        }
        if (server != null) {
            server.stop();
        }
    }

    @BeforeEach
    void setUp() {
        template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        template.getConnectionFactory().getConnection().serverCommands().flushAll();

        adapter = new RedisOtpStoreAdapter(template);
    }

    private static OtpChallenge challenge() {
        return new OtpChallenge(CHALLENGE_ID, 42L, Role.EDITOR, "hashed:123456");
    }

    @Test
    void savesAndReadsBackTheChallenge() {
        adapter.save(challenge(), TTL);

        OtpChallenge found = adapter.find(CHALLENGE_ID).orElseThrow();
        assertThat(found.challengeId()).isEqualTo(CHALLENGE_ID);
        assertThat(found.userId()).isEqualTo(42L);
        assertThat(found.role()).isEqualTo(Role.EDITOR);
        assertThat(found.otpHash()).isEqualTo("hashed:123456");
    }

    @Test
    void saveSetsTheTtl() {
        adapter.save(challenge(), TTL);

        Long ttl = template.getExpire(CHALLENGE_KEY, TimeUnit.MILLISECONDS);
        assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(TTL.toMillis());
    }

    @Test
    void unknownChallengeIsEmpty() {
        assertThat(adapter.find("no-such-challenge")).isEmpty();
    }

    @Test
    void deleteRemovesChallengeAndCounter() {
        adapter.save(challenge(), TTL);
        adapter.recordFailedAttempt(CHALLENGE_ID);

        adapter.delete(CHALLENGE_ID);

        assertThat(adapter.find(CHALLENGE_ID)).isEmpty();
        assertThat(template.hasKey(ATTEMPTS_KEY)).isFalse();
    }

    @Test
    void failedAttemptsCountUpAndExpireWithTheChallenge() {
        adapter.save(challenge(), TTL);

        assertThat(adapter.recordFailedAttempt(CHALLENGE_ID)).isEqualTo(1L);
        assertThat(adapter.recordFailedAttempt(CHALLENGE_ID)).isEqualTo(2L);

        // The counter must never outlive the challenge it guards, or a stale
        // count would poison a later challenge reusing the id.
        Long ttl = template.getExpire(ATTEMPTS_KEY, TimeUnit.MILLISECONDS);
        assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(TTL.toMillis());
    }

    @Test
    void challengeAutoExpiresAfterTtl() {
        adapter.save(challenge(), Duration.ofSeconds(1));
        assertThat(adapter.find(CHALLENGE_ID)).isPresent();

        await().atMost(Duration.ofSeconds(5))
                .until(() -> adapter.find(CHALLENGE_ID).isEmpty());
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
