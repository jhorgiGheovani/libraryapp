package com.jhorgi.libraryapp.adapter.out.redis;

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
 * Exercises the real Redis behaviour the unit tests (which use a fake) cannot:
 * the {@code increment} counter, the first-hit {@code expire}, the two-key
 * lock/attempts dance, and TTL-driven auto-unlock.
 *
 * Runs against an in-process embedded Redis (no Docker), on a random free port
 * so it never collides with a locally running Redis.
 */
class RedisLoginAttemptAdapterTest {

    private static final int MAX_ATTEMPTS = 3;
    private static final long WINDOW_MS = 60_000L;
    private static final long LOCK_MS = 60_000L;

    private static final String ID = "alice@example.com";
    private static final String ATTEMPTS_KEY = "login:attempts:" + ID;
    private static final String LOCK_KEY = "login:lock:" + ID;

    private static RedisServer server;
    private static int port;
    private static LettuceConnectionFactory factory;

    private StringRedisTemplate template;
    private RedisLoginAttemptAdapter adapter;

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

        adapter = new RedisLoginAttemptAdapter(template, MAX_ATTEMPTS, WINDOW_MS, LOCK_MS);
    }

    @Test
    void firstFailureCreatesCounterWithWindowTtl() {
        adapter.recordFailure(ID);

        assertThat(template.opsForValue().get(ATTEMPTS_KEY)).isEqualTo("1");
        Long ttl = template.getExpire(ATTEMPTS_KEY, TimeUnit.MILLISECONDS);
        assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(WINDOW_MS);
        assertThat(adapter.isLocked(ID)).isFalse();
    }

    @Test
    void countsUpButDoesNotLockBelowThreshold() {
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            adapter.recordFailure(ID);
        }

        assertThat(template.opsForValue().get(ATTEMPTS_KEY))
                .isEqualTo(String.valueOf(MAX_ATTEMPTS - 1));
        assertThat(adapter.isLocked(ID)).isFalse();
    }

    @Test
    void reachingThresholdLocksAndClearsCounter() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            adapter.recordFailure(ID);
        }

        assertThat(adapter.isLocked(ID)).isTrue();
        // counter is consumed when the lock is set
        assertThat(template.hasKey(ATTEMPTS_KEY)).isFalse();
        Long lockTtl = template.getExpire(LOCK_KEY, TimeUnit.MILLISECONDS);
        assertThat(lockTtl).isNotNull().isPositive().isLessThanOrEqualTo(LOCK_MS);
    }

    @Test
    void resetClearsBothKeys() {
        adapter.recordFailure(ID);
        adapter.reset(ID);

        assertThat(template.hasKey(ATTEMPTS_KEY)).isFalse();
        assertThat(adapter.isLocked(ID)).isFalse();
    }

    @Test
    void lockAutoExpiresAfterTtl() {
        // Short-lived lock so we can observe Redis expiring it on its own — this
        // is exactly the TTL behaviour that made keys "disappear" during manual
        // testing.
        RedisLoginAttemptAdapter shortLock =
                new RedisLoginAttemptAdapter(template, MAX_ATTEMPTS, WINDOW_MS, 1_000L);
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            shortLock.recordFailure(ID);
        }
        assertThat(shortLock.isLocked(ID)).isTrue();

        await().atMost(Duration.ofSeconds(5))
                .until(() -> !shortLock.isLocked(ID));
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
