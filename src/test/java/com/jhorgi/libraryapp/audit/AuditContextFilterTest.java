package com.jhorgi.libraryapp.audit;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditContextFilterTest {

    private static final String REMOTE = "203.0.113.7";
    private static final String CHROME =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/128.0.0.0 Safari/537.36";

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void tearDown() {
        AuditContext.clear();
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/articles");
        request.setRemoteAddr(REMOTE);
        request.addHeader(AuditContextFilter.USER_AGENT, CHROME);
        return request;
    }

    /** Captures what the context held while the rest of the chain was running. */
    private static MockFilterChain capturing(AtomicReference<AuditContext.RequestInfo> seen) {
        return new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                seen.set(AuditContext.current());
            }
        };
    }

    @Test
    void theAddressAndUserAgentAreVisibleForTheLengthOfTheRequest() throws Exception {
        AtomicReference<AuditContext.RequestInfo> seen = new AtomicReference<>();

        new AuditContextFilter(false).doFilter(request(), response, capturing(seen));

        assertThat(seen.get().ipAddress()).isEqualTo(REMOTE);
        assertThat(seen.get().userAgent()).isEqualTo(CHROME);
    }

    @Test
    void theContextIsClearedAfterwards() throws Exception {
        new AuditContextFilter(false).doFilter(request(), response, new MockFilterChain());

        // Tomcat pools threads. A context left behind is one the next unrelated
        // request inherits — audit rows blaming the wrong address.
        assertThat(AuditContext.current()).isEqualTo(AuditContext.NONE);
    }

    @Test
    void theContextIsClearedEvenWhenTheRequestBlowsUp() {
        MockFilterChain exploding = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                throw new IllegalStateException("boom");
            }
        };

        assertThatThrownBy(() -> new AuditContextFilter(false).doFilter(request(), response, exploding))
                .isInstanceOf(IllegalStateException.class);

        assertThat(AuditContext.current()).isEqualTo(AuditContext.NONE);
    }

    // ----- X-Forwarded-For -----

    @Test
    void aForwardedForHeaderIsIgnoredByDefault() throws Exception {
        // The header is client-settable. Trusting it out of the box would let
        // anyone forge the one column the trail exists to be honest about.
        MockHttpServletRequest spoofed = request();
        spoofed.addHeader(AuditContextFilter.FORWARDED_FOR, "1.2.3.4");
        AtomicReference<AuditContext.RequestInfo> seen = new AtomicReference<>();

        new AuditContextFilter(false).doFilter(spoofed, response, capturing(seen));

        assertThat(seen.get().ipAddress()).isEqualTo(REMOTE);
    }

    @Test
    void aForwardedForHeaderIsUsedWhenAProxyIsDeclaredTrusted() throws Exception {
        MockHttpServletRequest forwarded = request();
        forwarded.addHeader(AuditContextFilter.FORWARDED_FOR, "1.2.3.4");
        AtomicReference<AuditContext.RequestInfo> seen = new AtomicReference<>();

        new AuditContextFilter(true).doFilter(forwarded, response, capturing(seen));

        assertThat(seen.get().ipAddress()).isEqualTo("1.2.3.4");
    }

    @Test
    void theFirstHopIsTheClientWhenSeveralProxiesAppended() throws Exception {
        MockHttpServletRequest forwarded = request();
        forwarded.addHeader(AuditContextFilter.FORWARDED_FOR, "1.2.3.4, 10.0.0.1, 10.0.0.2");
        AtomicReference<AuditContext.RequestInfo> seen = new AtomicReference<>();

        new AuditContextFilter(true).doFilter(forwarded, response, capturing(seen));

        assertThat(seen.get().ipAddress()).isEqualTo("1.2.3.4");
    }

    @Test
    void aTrustedProxyWithNoForwardedHeaderFallsBackToTheSocketAddress() throws Exception {
        AtomicReference<AuditContext.RequestInfo> seen = new AtomicReference<>();

        new AuditContextFilter(true).doFilter(request(), response, capturing(seen));

        assertThat(seen.get().ipAddress()).isEqualTo(REMOTE);
    }

    // ----- hostile input -----

    @Test
    void anAbsurdlyLongUserAgentIsTruncatedRatherThanStoredWhole() throws IOException, ServletException {
        MockHttpServletRequest huge = new MockHttpServletRequest("GET", "/articles");
        huge.setRemoteAddr(REMOTE);
        huge.addHeader(AuditContextFilter.USER_AGENT, "x".repeat(5_000));
        AtomicReference<AuditContext.RequestInfo> seen = new AtomicReference<>();

        new AuditContextFilter(false).doFilter(huge, response, capturing(seen));

        // The column is 512; an untruncated header would fail the insert, which
        // would lose the entry entirely.
        assertThat(seen.get().userAgent()).hasSize(512);
    }

    @Test
    void aMissingUserAgentIsNullRatherThanBlank() throws Exception {
        MockHttpServletRequest bare = new MockHttpServletRequest("GET", "/articles");
        bare.setRemoteAddr(REMOTE);
        AtomicReference<AuditContext.RequestInfo> seen = new AtomicReference<>();

        new AuditContextFilter(false).doFilter(bare, response, capturing(seen));

        assertThat(seen.get().userAgent()).isNull();
        assertThat(seen.get().ipAddress()).isEqualTo(REMOTE);
    }
}
