package com.bmart.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:anilworks321@gmail.com}") String fromEmail
    ) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    public boolean sendOtpEmail(String toEmail, String otp, String purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("B-MART Security Verification Code");
            message.setText("Hello,\n\nYour B-MART verification code for " + purpose + " is: " + otp + "\n\nThis code is valid for 10 minutes. Do not share this code with anyone.\n\nThank you,\nB-MART Team");
            
            mailSender.send(message);
            log.info("Successfully sent OTP email to {} with OTP: {}", toEmail, otp);
            return true;
        } catch (Exception e) {
            log.error("Could not send email via SMTP to {}: {}. Logged OTP for dev/testing: {}", toEmail, e.getMessage(), otp, e);
            return false;
        }
    }
}
