package com.jhorgi.libraryapp.adapter.out.email;

import com.jhorgi.libraryapp.domain.port.out.EmailSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Sends the OTP over SMTP when mail is configured. Spring Boot only creates a
 * {@link JavaMailSender} if {@code spring.mail.host} is set, so with no mail
 * config the adapter logs the code instead — that keeps the app runnable (and
 * demoable) on a laptop without an SMTP server.
 */
@Component
public class EmailOtpSenderAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpSenderAdapter.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public EmailOtpSenderAdapter(ObjectProvider<JavaMailSender> mailSender,
                                 @Value("${security.otp.mail-from:no-reply@libraryapp.local}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendOtp(String toEmail, String code, Duration validFor) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("No mail sender configured; OTP for {} is {} (valid {} minutes)",
                    toEmail, code, validFor.toMinutes());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("Your login verification code");
        message.setText("Your verification code is " + code + ". It expires in "
                + validFor.toMinutes() + " minutes. If you did not try to log in, ignore this email.");
        sender.send(message);
        log.debug("OTP email sent to {}", toEmail);
    }
}
