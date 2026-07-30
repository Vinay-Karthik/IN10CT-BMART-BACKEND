package com.bmart.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    @Value("${sms.provider:MOCK}")
    private String smsProvider;

    public void sendSmsOtp(String phoneNumber, String otp) {
        if ("TWILIO".equalsIgnoreCase(smsProvider)) {
            log.info("[Twilio SMS Integration] Sending OTP {} to phone number {}", otp, phoneNumber);
            // Twilio SDK call placeholder
        } else if ("MSG91".equalsIgnoreCase(smsProvider)) {
            log.info("[MSG91 SMS Integration] Sending OTP {} to phone number {}", otp, phoneNumber);
            // MSG91 API call placeholder
        } else {
            log.info("[Mock SMS Gateway] OTP for phone {} is: {}", phoneNumber, otp);
        }
    }
}
