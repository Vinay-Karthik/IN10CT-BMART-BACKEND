package com.bmart.controller;

import com.bmart.dto.*;
import com.bmart.entity.User;
import com.bmart.repository.UserRepository;
import com.bmart.service.AuthService;
import com.bmart.service.LogoutService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LogoutService logoutService;
    private final UserRepository userRepository;

    private void setAuthCookie(HttpServletResponse response, String token) {
        Cookie authCookie = new Cookie("bmart_token", token);
        authCookie.setMaxAge(86400); // 1 day
        authCookie.setPath("/");
        response.addCookie(authCookie);

        String cookieHeader = String.format("bmart_token=%s; Path=/; Max-Age=86400; SameSite=Lax", token);
        response.addHeader("Set-Cookie", cookieHeader);
    }

    private void clearAuthCookie(HttpServletResponse response) {
        Cookie authCookie = new Cookie("bmart_token", "");
        authCookie.setMaxAge(0);
        authCookie.setPath("/");
        response.addCookie(authCookie);

        String cookieHeader = "bmart_token=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Lax";
        response.addHeader("Set-Cookie", cookieHeader);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request, HttpServletResponse response) {
        ApiResponse<AuthResponse> res = authService.verifyRegistrationOtp(request);
        if (res.isSuccess() && res.getData() != null && res.getData().getToken() != null) {
            setAuthCookie(response, res.getData().getToken());
        }
        return ResponseEntity.ok(res);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOtp(@RequestParam String target, @RequestParam(required = false) String type) {
        return ResponseEntity.ok(authService.resendOtp(target, type));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        ApiResponse<AuthResponse> res = authService.login(request);
        if (res.isSuccess() && res.getData() != null && res.getData().getToken() != null) {
            setAuthCookie(response, res.getData().getToken());
        }
        return ResponseEntity.ok(res);
    }

    @PostMapping("/otp-login/request")
    public ResponseEntity<ApiResponse<String>> requestOtpLogin(@RequestParam String target) {
        return ResponseEntity.ok(authService.requestOtpLogin(target));
    }

    @PostMapping("/otp-login/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtpLogin(@Valid @RequestBody OtpVerifyRequest request, HttpServletResponse response) {
        ApiResponse<AuthResponse> res = authService.verifyOtpLogin(request);
        if (res.isSuccess() && res.getData() != null && res.getData().getToken() != null) {
            setAuthCookie(response, res.getData().getToken());
        }
        return ResponseEntity.ok(res);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestParam String refreshToken, HttpServletResponse response) {
        ApiResponse<AuthResponse> res = authService.refreshToken(refreshToken);
        if (res.isSuccess() && res.getData() != null && res.getData().getToken() != null) {
            setAuthCookie(response, res.getData().getToken());
        }
        return ResponseEntity.ok(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            if (userDetails != null) {
                User user = userRepository.findByEmailOrUsername(userDetails.getUsername(), userDetails.getUsername()).orElse(null);
                if (user != null) {
                    logoutService.logout(user.getUserId());
                }
            }

            clearAuthCookie(response);

            Map<String, String> body = new HashMap<>();
            body.put("message", "Logout successful");
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            clearAuthCookie(response);
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", "Logout failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }
}
