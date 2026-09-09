package com.jhorgi.libraryapp.fake;

import com.jhorgi.libraryapp.domain.port.out.EmailSenderPort;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class FakeEmailSender implements EmailSenderPort {

    public record SentOtp(String toEmail, String code) {
    }

    private final List<SentOtp> sent = new ArrayList<>();

    @Override
    public void sendOtp(String toEmail, String code, Duration validFor) {
        sent.add(new SentOtp(toEmail, code));
    }

    public List<SentOtp> sent() {
        return sent;
    }

    public SentOtp last() {
        return sent.get(sent.size() - 1);
    }
}
