package com.bmart.controller;

import com.bmart.dto.ApiResponse;
import com.bmart.entity.OrderItem;
import com.bmart.entity.ReturnRequest;
import com.bmart.service.SellerOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SELLER') or hasRole('SELLER') or hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
public class SellerOrderController {

    private final SellerOrderService sellerOrderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderItem>>> getMyOrderItems(@AuthenticationPrincipal UserDetails userDetails) {
        List<OrderItem> items = sellerOrderService.getSellerOrderItems(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Seller order items retrieved", items));
    }

    @PutMapping("/{orderItemId}/status")
    public ResponseEntity<ApiResponse<OrderItem>> updateOrderStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderItemId,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Status field is required"));
        }
        try {
            OrderItem updated = sellerOrderService.updateOrderStatus(userDetails.getUsername(), orderItemId, status);
            return ResponseEntity.ok(ApiResponse.success("Order item status updated to " + updated.getStatus(), updated));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping("/{orderItemId}/return")
    public ResponseEntity<ApiResponse<ReturnRequest>> initiateReturn(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderItemId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.get("reason") : "Return requested by seller";
        ReturnRequest request = sellerOrderService.initiateReturn(userDetails.getUsername(), orderItemId, reason);
        return ResponseEntity.ok(ApiResponse.success("Return request submitted for admin review", request));
    }

    @GetMapping("/{orderItemId}/invoice")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInvoice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderItemId
    ) {
        Map<String, Object> invoice = sellerOrderService.generateInvoice(userDetails.getUsername(), orderItemId);
        return ResponseEntity.ok(ApiResponse.success("Invoice generated successfully", invoice));
    }
}
