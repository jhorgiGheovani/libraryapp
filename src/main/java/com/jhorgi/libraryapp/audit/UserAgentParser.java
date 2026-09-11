package com.jhorgi.libraryapp.audit;

import com.jhorgi.libraryapp.domain.model.DeviceInfo;

import java.util.Locale;

/**
 * Turns a {@code User-Agent} header into browser / OS / device.
 *
 * <p>Hand-rolled rather than yauaa or uap-java. Those ship a rules database that
 * needs updating to stay accurate, and they earn that weight when the parse
 * drives a decision — ours drives a column that a human reads. The raw header is
 * stored verbatim next to the parse, so a wrong guess is recoverable and a new
 * browser is a one-line addition here, not a stale dependency.
 *
 * <p>Order is load-bearing. Every browser lies about being every other browser:
 * Edge's header contains "Chrome" and "Safari", Chrome's contains "Safari". The
 * most specific claim has to be tested first, so the checks run narrow to broad.
 */
public final class UserAgentParser {

    private UserAgentParser() {
    }

    public static DeviceInfo parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return DeviceInfo.unknown();
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        return new DeviceInfo(browser(ua), operatingSystem(ua), device(ua));
    }

    private static String browser(String ua) {
        if (isBot(ua)) {
            return "Bot";
        }
        if (ua.contains("edg/") || ua.contains("edge/") || ua.contains("edga/") || ua.contains("edgios/")) {
            return "Edge";
        }
        if (ua.contains("opr/") || ua.contains("opera")) {
            return "Opera";
        }
        if (ua.contains("samsungbrowser")) {
            return "Samsung Internet";
        }
        if (ua.contains("firefox/") || ua.contains("fxios/")) {
            return "Firefox";
        }
        if (ua.contains("chrome/") || ua.contains("crios/")) {
            return "Chrome";
        }
        // Only reached once every Chromium-based claim above has been ruled out.
        if (ua.contains("safari/")) {
            return "Safari";
        }
        if (ua.contains("postmanruntime")) {
            return "Postman";
        }
        if (ua.contains("curl/")) {
            return "curl";
        }
        if (ua.contains("insomnia")) {
            return "Insomnia";
        }
        return DeviceInfo.UNKNOWN;
    }

    private static String operatingSystem(String ua) {
        // Android before Linux: every Android header also says "linux".
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) {
            return "iOS";
        }
        if (ua.contains("windows")) {
            return "Windows";
        }
        // "mac os x" also appears on iOS iPad headers, which the check above caught.
        if (ua.contains("mac os") || ua.contains("macintosh")) {
            return "macOS";
        }
        if (ua.contains("cros")) {
            return "ChromeOS";
        }
        if (ua.contains("linux") || ua.contains("x11")) {
            return "Linux";
        }
        return DeviceInfo.UNKNOWN;
    }

    private static String device(String ua) {
        if (isBot(ua)) {
            return "Bot";
        }
        if (ua.contains("ipad") || ua.contains("tablet") || (ua.contains("android") && !ua.contains("mobile"))) {
            return "Tablet";
        }
        if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("ipod") || ua.contains("android")) {
            return "Mobile";
        }
        if (ua.contains("postmanruntime") || ua.contains("curl/") || ua.contains("insomnia")
                || ua.contains("java/") || ua.contains("okhttp") || ua.contains("python-requests")) {
            return "API Client";
        }
        return "Desktop";
    }

    private static boolean isBot(String ua) {
        return ua.contains("bot") || ua.contains("crawler") || ua.contains("spider")
                || ua.contains("slurp");
    }
}
