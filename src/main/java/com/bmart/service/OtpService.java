package com.bmart.service;

import com.bmart.entity.OtpVerification;
import com.bmart.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final SecureRandom random = new SecureRandom();

    public String generateAndSendOtp(String target, String type) {
        String otp = String.format("%06d", random.nextInt(1000000));
        
        OtpVerification otpVerification = OtpVerification.builder()
                .target(target)
                .otp(otp)
                .type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .isUsed(false)
                .build();
        
        otpRepository.save(otpVerification);

        if (target.contains("@")) {
            emailService.sendOtpEmail(target, otp, type);
        } else {
            smsService.sendSmsOtp(target, otp);
        }

        return otp;
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
