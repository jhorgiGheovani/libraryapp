package com.jhorgi.libraryapp.audit;

import com.jhorgi.libraryapp.domain.model.DeviceInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAgentParserTest {

    private static final String CHROME_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/128.0.0.0 Safari/537.36";
    private static final String SAFARI_MAC =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) "
                    + "Version/17.0 Safari/605.1.15";
    private static final String EDGE_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/128.0.0.0 Safari/537.36 Edg/128.0.0.0";
    private static final String SAFARI_IPHONE =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) "
                    + "Version/17.0 Mobile/15E148 Safari/604.1";
    private static final String CHROME_ANDROID_PHONE =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/128.0.0.0 Mobile Safari/537.36";
    private static final String CHROME_ANDROID_TABLET =
            "Mozilla/5.0 (Linux; Android 13; SM-X700) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/128.0.0.0 Safari/537.36";
    private static final String FIREFOX_LINUX =
            "Mozilla/5.0 (X11; Linux x86_64; rv:129.0) Gecko/20100101 Firefox/129.0";
    private static final String GOOGLEBOT =
            "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";

    // ----- the ordering traps -----

    @Test
    void edgeIsNotReportedAsChromeEvenThoughItsHeaderSaysChrome() {
        // Every Chromium browser claims to be Chrome AND Safari. If the checks ran
        // broad-to-narrow, every Edge user in the trail would read as Chrome.
        assertThat(UserAgentParser.parse(EDGE_WINDOWS).browser()).isEqualTo("Edge");
    }

    @Test
    void chromeIsNotReportedAsSafari() {
        assertThat(UserAgentParser.parse(CHROME_WINDOWS).browser()).isEqualTo("Chrome");
    }

    @Test
    void realSafariIsStillRecognised() {
        DeviceInfo info = UserAgentParser.parse(SAFARI_MAC);

        assertThat(info.browser()).isEqualTo("Safari");
        assertThat(info.operatingSystem()).isEqualTo("macOS");
        assertThat(info.device()).isEqualTo("Desktop");
    }

    @Test
    void androidIsNotReportedAsLinuxEvenThoughItsHeaderSaysLinux() {
        assertThat(UserAgentParser.parse(CHROME_ANDROID_PHONE).operatingSystem()).isEqualTo("Android");
    }

    @Test
    void anIphoneIsIosNotMacOsEvenThoughItsHeaderSaysMacOsX() {
        assertThat(UserAgentParser.parse(SAFARI_IPHONE).operatingSystem()).isEqualTo("iOS");
    }

    // ----- device class -----

    @Test
    void anAndroidHeaderWithoutMobileIsATablet() {
        // This is the actual signal Android tablets use: they drop the "Mobile"
        // token and are otherwise identical to a phone.
        assertThat(UserAgentParser.parse(CHROME_ANDROID_TABLET).device()).isEqualTo("Tablet");
        assertThat(UserAgentParser.parse(CHROME_ANDROID_PHONE).device()).isEqualTo("Mobile");
    }

    @Test
    void aDesktopBrowserIsADesktop() {
        assertThat(UserAgentParser.parse(FIREFOX_LINUX).device()).isEqualTo("Desktop");
        assertThat(UserAgentParser.parse(FIREFOX_LINUX).browser()).isEqualTo("Firefox");
        assertThat(UserAgentParser.parse(FIREFOX_LINUX).operatingSystem()).isEqualTo("Linux");
    }

    @Test
    void apiClientsAreNotCountedAsBrowsers() {
        assertThat(UserAgentParser.parse("PostmanRuntime/7.39.0").browser()).isEqualTo("Postman");
        assertThat(UserAgentParser.parse("PostmanRuntime/7.39.0").device()).isEqualTo("API Client");
        assertThat(UserAgentParser.parse("curl/8.7.1").device()).isEqualTo("API Client");
    }

    @Test
    void botsAreLabelledAsSuch() {
        DeviceInfo info = UserAgentParser.parse(GOOGLEBOT);

        assertThat(info.browser()).isEqualTo("Bot");
        assertThat(info.device()).isEqualTo("Bot");
    }

    // ----- absent or unrecognised -----

    @Test
    void aMissingOrBlankHeaderIsUnknownRatherThanAnException() {
        // A client is free to send no User-Agent at all. The entry still has to be
        // written: an unparseable header is not a reason to lose the IP and the
        // timestamp.
        assertThat(UserAgentParser.parse(null)).isEqualTo(DeviceInfo.unknown());
        assertThat(UserAgentParser.parse("")).isEqualTo(DeviceInfo.unknown());
        assertThat(UserAgentParser.parse("   ")).isEqualTo(DeviceInfo.unknown());
    }

    @Test
    void anUnrecognisedHeaderDegradesFieldByField() {
        DeviceInfo info = UserAgentParser.parse("SomeFutureBrowser/1.0 (Windows NT 11.0)");

        assertThat(info.browser()).isEqualTo(DeviceInfo.UNKNOWN);
        assertThat(info.operatingSystem()).isEqualTo("Windows");
    }

    @Test
    void parsingIsCaseInsensitive() {
        assertThat(UserAgentParser.parse(CHROME_WINDOWS.toUpperCase()).browser()).isEqualTo("Chrome");
    }
}
