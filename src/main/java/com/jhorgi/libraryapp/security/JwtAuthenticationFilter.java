package com.jhorgi.libraryapp.security;

import com.jhorgi.libraryapp.domain.model.TokenPayload;
import com.jhorgi.libraryapp.domain.port.out.TokenPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final TokenPort tokens;

    public JwtAuthenticationFilter(TokenPort tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            TokenPayload payload = tokens.verify(header.substring(PREFIX.length()).trim());
            AuthenticatedUser principal = new AuthenticatedUser(payload.userId(), payload.role());
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + payload.role().name()));
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, authorities));
        } catch (RuntimeException ex) {

            SecurityContextHolder.clearContext();
            log.debug("Rejected bearer token: {}", ex.getMessage());
        }

        chain.doFilter(request, response);
    }
}
