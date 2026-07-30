package com.bmart.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerifyRequest {

    @NotBlank(message = "Target (email or phone) is required")
    private String target;

    @NotBlank(message = "OTP is required")
    private String otp;

    private String type; // REGISTRATION, LOGIN, FORGOT_PASSWORD
}
