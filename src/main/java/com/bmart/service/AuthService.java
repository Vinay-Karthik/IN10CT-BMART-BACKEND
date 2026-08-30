package com.bmart.service;

import com.bmart.dto.*;
import com.bmart.entity.User;
import com.bmart.repository.UserRepository;
import com.bmart.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final OtpService otpService;
    private final com.bmart.repository.JwtTokenRepository jwtTokenRepository;

    private AuthResponse buildAuthResponse(User user, String token, String refreshToken) {
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .city(user.getCity())
                .state(user.getState())
                .pincode(user.getPincode())
                .build();
    }

    public ApiResponse<String> register(RegisterRequest request) {
        Optional<User> existingByEmail = userRepository.findByEmail(request.getEmail());
        Optional<User> existingByUsername = userRepository.findByUsername(request.getUsername());

        // Block registration ONLY if an account is already verified
        if (existingByEmail.isPresent() && existingByEmail.get().isVerified()) {
            return ApiResponse.error("Email address is already registered");
        }
        if (existingByUsername.isPresent() && existingByUsername.get().isVerified()) {
            return ApiResponse.error("Username is already taken");
        }

        // Reuse unverified user account or create new one
        User user = existingByEmail.or(() -> existingByUsername).orElseGet(User::new);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setFullName(request.getFullName() != null ? request.getFullName() : request.getUsername());
        user.setRole("CUSTOMER");
        user.setVerified(false);

        userRepository.save(user);
        OtpService.OtpResult result = otpService.generateAndSendOtpDetails(user.getEmail(), "REGISTRATION");

        if (result.isEmailSent()) {
            return ApiResponse.success("Registration successful. Please verify the OTP code sent to your email.");
        } else {
            return ApiResponse.success("Registration successful. (Email delivery delayed/failed; Dev OTP: " + result.getOtp() + ")");
        }
    }

    public ApiResponse<AuthResponse> verifyRegistrationOtp(OtpVerifyRequest request) {
        String target = request.getTarget() != null ? request.getTarget().trim() : "";
        boolean isValid = otpService.verifyOtp(target, request.getOtp(), "REGISTRATION");
        if (!isValid) {
            return ApiResponse.error("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(target)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setVerified(true);
        userRepository.save(user);

        String token = tokenProvider.generateTokenFromUsernameAndRole(user.getEmail(), user.getRole());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        // Persist token for DB-backed revocation tracking
        jwtTokenRepository.deleteByUserId(user.getUserId());
        jwtTokenRepository.save(com.bmart.entity.JwtToken.builder()
                .userId(user.getUserId())
                .token(token)
                .build());

        return ApiResponse.success("Account verified successfully", buildAuthResponse(user, token, refreshToken));
    }

    public ApiResponse<String> resendOtp(String target, String type) {
        OtpService.OtpResult result = otpService.generateAndSendOtpDetails(target, type != null ? type : "REGISTRATION");
        if (result.isEmailSent()) {
            return ApiResponse.success("Verification code resent successfully to your email address.");
        } else {
            return ApiResponse.success("Verification code generated. (Email delivery delayed/failed; Dev OTP: " + result.getOtp() + ")");
        }
    }

    public ApiResponse<AuthResponse> login(LoginRequest request) {
        String input = request.getEmailOrUsername() != null ? request.getEmailOrUsername().trim() : "";
        String rawPassword = request.getPassword() != null ? request.getPassword() : "";

        User user = userRepository.findByEmailOrUsername(input, input)
                .orElse(null);

        if (user == null) {
            return ApiResponse.error("No account found with username or email: " + input);
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            return ApiResponse.error("Invalid password. Please check your credentials.");
        }

        if (!user.isVerified()) {
            user.setVerified(true);
            userRepository.save(user);
        }

        String token = tokenProvider.generateTokenFromUsernameAndRole(user.getEmail(), user.getRole());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        // Persist token for DB-backed revocation tracking
        jwtTokenRepository.deleteByUserId(user.getUserId());
        jwtTokenRepository.save(com.bmart.entity.JwtToken.builder()
                .userId(user.getUserId())
                .token(token)
                .build());

        return ApiResponse.success("Login successful", buildAuthResponse(user, token, refreshToken));
    }

    public ApiResponse<String> requestOtpLogin(String target) {
        User user = userRepository.findByEmailOrUsername(target, target)
                .orElse(null);
        if (user == null) {
            return ApiResponse.error("No registered account found with " + target);
        }
        OtpService.OtpResult result = otpService.generateAndSendOtpDetails(user.getEmail(), "LOGIN");
        if (result.isEmailSent()) {
            return ApiResponse.success("OTP sent for login to your email address.");
        } else {
            return ApiResponse.success("OTP generated for login. (Email delivery delayed/failed; Dev OTP: " + result.getOtp() + ")");
        }
    }

    public ApiResponse<AuthResponse> verifyOtpLogin(OtpVerifyRequest request) {
        String target = request.getTarget() != null ? request.getTarget().trim() : "";
        User user = userRepository.findByEmailOrUsername(target, target)
                .orElse(null);

        if (user == null) {
            return ApiResponse.error("User account not found for: " + target);
        }

        boolean isValid = otpService.verifyOtp(user.getEmail(), request.getOtp(), "LOGIN")
                || otpService.verifyOtp(target, request.getOtp(), "LOGIN");

        if (!isValid) {
            return ApiResponse.error("Invalid or expired OTP");
        }

        if (!user.isVerified()) {
            user.setVerified(true);
            userRepository.save(user);
        }

        String token = tokenProvider.generateTokenFromUsernameAndRole(user.getEmail(), user.getRole());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        // Persist token for DB-backed revocation tracking
        jwtTokenRepository.deleteByUserId(user.getUserId());
        jwtTokenRepository.save(com.bmart.entity.JwtToken.builder()
                .userId(user.getUserId())
                .token(token)
                .build());

        return ApiResponse.success("OTP login successful", buildAuthResponse(user, token, refreshToken));
    }

    public ApiResponse<String> forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);
        if (user == null) {
            return ApiResponse.error("No user found with email " + request.getEmail());
        }
        otpService.generateAndSendOtp(user.getEmail(), "FORGOT_PASSWORD");
        return ApiResponse.success("Password reset OTP sent to email");
    }

    public ApiResponse<String> resetPassword(ResetPasswordRequest request) {
        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp(), "FORGOT_PASSWORD");
        if (!isValid) {
            return ApiResponse.error("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ApiResponse.success("Password reset successfully. You can now login with your new password.");
    }

    public ApiResponse<AuthResponse> refreshToken(String refreshToken) {
        if (tokenProvider.validateToken(refreshToken)) {
            String username = tokenProvider.getUsernameFromJWT(refreshToken);
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String newToken = tokenProvider.generateTokenFromUsernameAndRole(user.getEmail(), user.getRole());
            String newRefreshToken = tokenProvider.generateRefreshToken(user.getEmail());

            // Persist token for DB-backed revocation tracking
            jwtTokenRepository.deleteByUserId(user.getUserId());
            jwtTokenRepository.save(com.bmart.entity.JwtToken.builder()
                    .userId(user.getUserId())
                    .token(newToken)
                    .build());

            return ApiResponse.success("Token refreshed successfully", buildAuthResponse(user, newToken, newRefreshToken));
        }
        return ApiResponse.error("Invalid refresh token");
    }
}
