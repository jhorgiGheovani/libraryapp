package com.jhorgi.libraryapp.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lifts the client address and the user-agent off the request into
 * {@link AuditContext}, for the length of that request and no longer.
 *
 * <p>Registered first in the chain (see {@code AuditConfig}) so that anything
 * throwing later — a rejected token, a 403 from method security — still happens
 * with the context populated.
 */
public class AuditContextFilter extends OncePerRequestFilter {

    static final String FORWARDED_FOR = "X-Forwarded-For";
    static final String USER_AGENT = "User-Agent";

    /** Beyond this the header is junk or an attempt to bloat the row; store the prefix. */
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private final boolean trustForwardedFor;

    public AuditContextFilter(boolean trustForwardedFor) {
        this.trustForwardedFor = trustForwardedFor;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        AuditContext.set(new AuditContext.RequestInfo(clientIp(request), userAgent(request)));
        try {
            chain.doFilter(request, response);
        } finally {
            // Tomcat reuses threads. Without this the next request on this thread
            // inherits the previous caller's address.
            AuditContext.clear();
        }
    }

    /**
     * {@code X-Forwarded-For} is set by the client unless something in front is
     * overwriting it, so trusting it by default would let anyone forge the one
     * column the trail exists to be honest about. Off unless
     * {@code audit.trust-forwarded-for=true} says a proxy is really there.
     *
     * <p>When trusted, the first hop is used: proxies append, so the leftmost
     * entry is the original client.
     */
    private String clientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader(FORWARDED_FOR);
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static String userAgent(HttpServletRequest request) {
        String header = request.getHeader(USER_AGENT);
        if (header == null || header.isBlank()) {
            return null;
        }
        return header.length() > MAX_USER_AGENT_LENGTH
                ? header.substring(0, MAX_USER_AGENT_LENGTH)
                : header;
    }
}
