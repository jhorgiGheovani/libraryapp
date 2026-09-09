package com.jhorgi.libraryapp.adapter.out.token;

import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.TokenPayload;
import com.jhorgi.libraryapp.domain.port.out.TokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JjwtTokenAdapter implements TokenPort {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final long expirationMs;

    public JjwtTokenAdapter(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public String issue(Long userId, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    @Override
    public TokenPayload verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId = Long.valueOf(claims.getSubject());
        Role role = Role.valueOf(claims.get(ROLE_CLAIM, String.class));
        return new TokenPayload(userId, role);
    }
}
