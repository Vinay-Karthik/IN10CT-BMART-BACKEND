package com.bmart.controller;

import com.bmart.dto.ApiResponse;
import com.bmart.entity.OrderMessage;
import com.bmart.entity.Review;
import com.bmart.service.SellerInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SELLER') or hasRole('SELLER') or hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
public class SellerInteractionController {

    private final SellerInteractionService sellerInteractionService;

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<List<Review>>> getSellerReviews(@AuthenticationPrincipal UserDetails userDetails) {
        List<Review> reviews = sellerInteractionService.getSellerProductReviews(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Seller product reviews retrieved", reviews));
    }

    @PostMapping("/reviews/{id}/reply")
    public ResponseEntity<ApiResponse<Review>> replyToReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String replyComment = body.get("replyComment");
        if (replyComment == null || replyComment.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("replyComment field is required"));
        }
        Review updated = sellerInteractionService.replyToReview(userDetails.getUsername(), id, replyComment.trim());
        return ResponseEntity.ok(ApiResponse.success("Review reply saved successfully", updated));
    }

    @GetMapping("/messages/{orderId}")
    public ResponseEntity<ApiResponse<List<OrderMessage>>> getOrderMessages(@PathVariable String orderId) {
        List<OrderMessage> messages = sellerInteractionService.getOrderMessages(orderId);
        return ResponseEntity.ok(ApiResponse.success("Order messages retrieved", messages));
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<OrderMessage>> sendOrderMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body
    ) {
        String orderId = body.get("orderId");
        String messageText = body.get("message");

        if (orderId == null || messageText == null || messageText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("orderId and message fields are required"));
        }
        OrderMessage msg = sellerInteractionService.sendOrderMessage(userDetails.getUsername(), orderId, messageText.trim());
        return ResponseEntity.ok(ApiResponse.success("Order message sent successfully", msg));
    }
}
