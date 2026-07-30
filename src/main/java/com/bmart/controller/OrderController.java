package com.bmart.controller;

import com.bmart.dto.ApiResponse;
import com.bmart.dto.OrderRequest;
import com.bmart.entity.Order;
import com.bmart.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<Order>> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Order placed successfully", orderService.createOrderFromCart(userDetails.getUsername(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Order>>> getUserOrders(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("User orders retrieved", orderService.getUserOrders(userDetails.getUsername())));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Order>> getOrderById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String orderId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Order details retrieved", orderService.getOrderById(orderId, userDetails.getUsername())));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<Order>> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam String status
    ) {
        return ResponseEntity.ok(ApiResponse.success("Order status updated", orderService.updateOrderStatus(orderId, status)));
    }
}
