package com.jhorgi.libraryapp.adapter.out.redis;

import com.jhorgi.libraryapp.domain.port.out.LoginAttemptPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
@Component
public class RedisLoginAttemptAdapter implements LoginAttemptPort {

    private static final String ATTEMPTS_PREFIX = "login:attempts:";
    private static final String LOCK_PREFIX = "login:lock:";

    private final StringRedisTemplate redis;
    private final int maxAttempts;
    private final Duration window;
    private final Duration lockDuration;

    public RedisLoginAttemptAdapter(
            StringRedisTemplate redis,
            @Value("${security.lockout.max-attempts:5}") int maxAttempts,
            @Value("${security.lockout.window-ms:900000}") long windowMs,
            @Value("${security.lockout.lock-ms:900000}") long lockMs) {
        this.redis = redis;
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofMillis(windowMs);
        this.lockDuration = Duration.ofMillis(lockMs);
    }

    @Override
    public boolean isLocked(String identifier) {
        return Boolean.TRUE.equals(redis.hasKey(lockKey(identifier)));
    }

    @Override
    public void recordFailure(String identifier) {
        String attemptsKey = attemptsKey(identifier);
        Long count = redis.opsForValue().increment(attemptsKey);
        if (count == null) {
            return;
        }
        if (count == 1L) {
            redis.expire(attemptsKey, window);
        }
        if (count >= maxAttempts) {
            redis.opsForValue().set(lockKey(identifier), "1", lockDuration);
            redis.delete(attemptsKey);
        }
    }

    @Override
    public void reset(String identifier) {
        redis.delete(attemptsKey(identifier));
        redis.delete(lockKey(identifier));
    }

    private String attemptsKey(String identifier) {
        return ATTEMPTS_PREFIX + identifier;
    }

    private String lockKey(String identifier) {
        return LOCK_PREFIX + identifier;
    }
}
