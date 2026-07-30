package com.bmart.controller;

import com.bmart.dto.ApiResponse;
import com.bmart.dto.PaymentVerificationRequest;
import com.bmart.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyPayment(@Valid @RequestBody PaymentVerificationRequest request) {
        boolean isSuccess = paymentService.verifyPayment(request);
        if (isSuccess) {
            return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", true));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Payment verification failed"));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<String>> handleWebhook(@RequestBody String payload, @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        // Webhook handler for Razorpay automated event updates
        return ResponseEntity.ok(ApiResponse.success("Webhook received"));
    }
}
