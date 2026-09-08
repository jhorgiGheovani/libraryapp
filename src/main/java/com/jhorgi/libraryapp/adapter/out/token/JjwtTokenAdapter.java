package com.jhorgi.libraryapp.adapter.out.token;

import com.jhorgi.libraryapp.domain.model.Role;
import com.jhorgi.libraryapp.domain.model.TokenPayload;
import com.jhorgi.libraryapp.domain.port.out.TokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JjwtTokenAdapter implements TokenPort {

    private static final String ROLE_CLAIM = "role";

    private final byte[] secret;
    private final long expirationMs;

    public JjwtTokenAdapter(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMs = expirationMs;
    }

    @Override
    public String issue(Long userId, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role.name())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    @Override
    public TokenPayload verify(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
        Long userId = Long.valueOf(claims.getSubject());
        Role role = Role.valueOf(claims.get(ROLE_CLAIM, String.class));
        return new TokenPayload(userId, role);
    }
}
