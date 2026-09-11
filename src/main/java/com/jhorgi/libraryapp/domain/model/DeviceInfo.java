package com.jhorgi.libraryapp.domain.model;

/**
 * The parsed half of a {@code User-Agent} header. The raw header is always kept
 * alongside it, so a wrong guess here loses nothing that cannot be recovered.
 */
public record DeviceInfo(String browser, String operatingSystem, String device) {

    public static final String UNKNOWN = "Unknown";

    public static DeviceInfo unknown() {
        return new DeviceInfo(UNKNOWN, UNKNOWN, UNKNOWN);
    }
}
