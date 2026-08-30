package com.bmart.service;

import com.bmart.entity.OtpVerification;
import com.bmart.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final SecureRandom random = new SecureRandom();

    public static class OtpResult {
        private final String otp;
        private final boolean emailSent;

        public OtpResult(String otp, boolean emailSent) {
            this.otp = otp;
            this.emailSent = emailSent;
        }

        public String getOtp() { return otp; }
        public boolean isEmailSent() { return emailSent; }
    }

    public OtpResult generateAndSendOtpDetails(String target, String type) {
        String otp = String.format("%06d", random.nextInt(1000000));
        
        OtpVerification otpVerification = OtpVerification.builder()
                .target(target)
                .otp(otp)
                .type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .isUsed(false)
                .build();
        
        otpRepository.save(otpVerification);

        log.info("\n========================================\n>>> B-MART VERIFICATION OTP FOR [{}]: [{}]\n========================================", target, otp);
        System.out.println(">>> B-MART VERIFICATION OTP FOR [" + target + "]: [" + otp + "]");

        boolean emailSent = true;
        if (target.contains("@")) {
            emailSent = emailService.sendOtpEmail(target, otp, type);
        } else {
            smsService.sendSmsOtp(target, otp);
        }

        return new OtpResult(otp, emailSent);
    }

    public String generateAndSendOtp(String target, String type) {
        return generateAndSendOtpDetails(target, type).getOtp();
    }

    public boolean verifyOtp(String target, String otp, String type) {
        return otpRepository.findTopByTargetAndTypeAndIsUsedFalseOrderByCreatedAtDesc(target, type)
                .map(otpRecord -> {
                    if (otpRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
                        return false;
                    }
                    if (otpRecord.getOtp().equals(otp)) {
                        otpRecord.setUsed(true);
                        otpRepository.save(otpRecord);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }
}
