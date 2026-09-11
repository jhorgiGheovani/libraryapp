package com.jhorgi.libraryapp.audit;

/**
 * The transport facts about the current request, parked where the audit
 * pipeline can reach them without every service signature growing an IP address
 * and a user-agent string.
 *
 * <p>A {@code ThreadLocal} rather than a request-scoped bean: the aspect runs
 * deep inside the call stack on the request thread, and a scoped proxy would
 * make every audited service depend on there being an HTTP request at all —
 * which breaks the moment the seeder or a test calls the same method.
 *
 * <p>{@link #clear()} in a {@code finally} is mandatory. Tomcat pools its
 * threads, so a context left behind is a context the next unrelated request
 * inherits, and audit rows that quietly attribute one user's address to another
 * are worse than no rows at all.
 */
public final class AuditContext {

    /** What a call outside any HTTP request sees: the seeder, a scheduled job, a test. */
    public static final RequestInfo NONE = new RequestInfo(null, null);

    private static final ThreadLocal<RequestInfo> CURRENT = new ThreadLocal<>();

    private AuditContext() {
    }

    public static void set(RequestInfo info) {
        CURRENT.set(info != null ? info : NONE);
    }

    public static RequestInfo current() {
        RequestInfo info = CURRENT.get();
        return info != null ? info : NONE;
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record RequestInfo(String ipAddress, String userAgent) {
    }
}
