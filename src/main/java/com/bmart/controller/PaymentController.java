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

    @GetMapping("/key")
    public ResponseEntity<ApiResponse<String>> getRazorpayKey() {
        return ResponseEntity.ok(ApiResponse.success("Razorpay key retrieved", paymentService.getRazorpayKeyId()));
    }

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
        boolean processed = paymentService.handleWebhook(payload, signature);
        if (processed) {
            return ResponseEntity.ok(ApiResponse.success("Webhook processed successfully"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Webhook signature verification failed"));
        }
    }

    @PostMapping(value = "/callback", consumes = org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void handlePaymentCallback(
            @RequestParam("razorpay_payment_id") String paymentId,
            @RequestParam("razorpay_order_id") String razorpayOrderId,
            @RequestParam("razorpay_signature") String signature,
            @RequestParam("order_id") String orderId,
            jakarta.servlet.http.HttpServletResponse response
    ) throws java.io.IOException {
        PaymentVerificationRequest verificationRequest = PaymentVerificationRequest.builder()
                .orderId(orderId)
                .razorpayOrderId(razorpayOrderId)
                .razorpayPaymentId(paymentId)
                .razorpaySignature(signature)
                .build();
        
        boolean isSuccess = paymentService.verifyPayment(verificationRequest);
        if (isSuccess) {
            response.sendRedirect("http://localhost:3000/order-confirmation/" + orderId);
        } else {
            response.sendRedirect("http://localhost:3000/payment-failure");
        }
    }
}
