package com.jhorgi.libraryapp.adapter.out.redis;

import com.jhorgi.libraryapp.domain.model.OtpChallenge;
import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.port.out.OtpStorePort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Component
public class RedisOtpStoreAdapter implements OtpStorePort {

    private static final String CHALLENGE_PREFIX = "otp:challenge:";
    private static final String ATTEMPTS_PREFIX = "otp:attempts:";

    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_ROLE = "role";
    private static final String FIELD_HASH = "otpHash";

    private final StringRedisTemplate redis;

    public RedisOtpStoreAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void save(OtpChallenge challenge, Duration ttl) {
        String key = challengeKey(challenge.challengeId());
        redis.opsForHash().putAll(key, Map.of(
                FIELD_USER_ID, String.valueOf(challenge.userId()),
                FIELD_ROLE, challenge.role().name(),
                FIELD_HASH, challenge.otpHash()));
        redis.expire(key, ttl);
    }

    @Override
    public Optional<OtpChallenge> find(String challengeId) {
        Map<Object, Object> fields = redis.opsForHash().entries(challengeKey(challengeId));
        if (fields.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new OtpChallenge(
                challengeId,
                Long.valueOf((String) fields.get(FIELD_USER_ID)),
                Role.valueOf((String) fields.get(FIELD_ROLE)),
                (String) fields.get(FIELD_HASH)));
    }

    @Override
    public void delete(String challengeId) {
        redis.delete(challengeKey(challengeId));
        redis.delete(attemptsKey(challengeId));
    }

    @Override
    public long recordFailedAttempt(String challengeId) {
        String key = attemptsKey(challengeId);
        Long count = redis.opsForValue().increment(key);
        if (count == null) {
            return 0L;
        }
        if (count == 1L) {
            // Tie the counter's life to the challenge it guards, so a stale
            // counter can never outlive it and poison a later challenge.
            Long remainingMs = redis.getExpire(challengeKey(challengeId), TimeUnit.MILLISECONDS);
            if (remainingMs != null && remainingMs > 0) {
                redis.expire(key, Duration.ofMillis(remainingMs));
            }
        }
        return count;
    }

    private String challengeKey(String challengeId) {
        return CHALLENGE_PREFIX + challengeId;
    }

    private String attemptsKey(String challengeId) {
        return ATTEMPTS_PREFIX + challengeId;
    }
}
