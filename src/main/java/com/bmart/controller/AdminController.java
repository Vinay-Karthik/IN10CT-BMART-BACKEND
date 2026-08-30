package com.bmart.controller;

import com.bmart.dto.ApiResponse;
import com.bmart.entity.*;
import com.bmart.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // Phase 8: User Management
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<User>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<User> users = adminService.getAllUsers(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("All users list", users));
    }

    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<ApiResponse<User>> suspendUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.get("reason") : "Suspended by admin";
        User user = adminService.suspendUser(userDetails.getUsername(), id, reason);
        return ResponseEntity.ok(ApiResponse.success("User account suspended", user));
    }

    @PutMapping("/users/{id}/ban")
    public ResponseEntity<ApiResponse<User>> banUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.get("reason") : "Banned by admin";
        User user = adminService.banUser(userDetails.getUsername(), id, reason);
        return ResponseEntity.ok(ApiResponse.success("User account banned", user));
    }

    @PutMapping("/users/{id}/activate")
    public ResponseEntity<ApiResponse<User>> activateUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        User user = adminService.activateUser(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("User account restored to active status", user));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<User>> changeRole(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String newRole = body.get("role");
        if (newRole == null || newRole.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Role field is required"));
        }
        User user = adminService.changeUserRole(userDetails.getUsername(), id, newRole);
        return ResponseEntity.ok(ApiResponse.success("User role updated to " + user.getRole(), user));
    }

    // Phase 9: Product & Catalog Control
    @GetMapping("/products/pending")
    public ResponseEntity<ApiResponse<List<Product>>> getPendingProducts() {
        return ResponseEntity.ok(ApiResponse.success("Pending product listings", adminService.getPendingProducts()));
    }

    @PutMapping("/products/{id}/approve")
    public ResponseEntity<ApiResponse<Product>> approveProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        Product product = adminService.approveProduct(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Product approved and live on marketplace", product));
    }

    @PutMapping("/products/{id}/ban")
    public ResponseEntity<ApiResponse<Product>> banProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.get("reason") : "Banned by admin";
        Product product = adminService.banProduct(userDetails.getUsername(), id, reason);
        return ResponseEntity.ok(ApiResponse.success("Product banned from platform", product));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<Product>> createProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody com.bmart.dto.SellerProductRequestDTO dto
    ) {
        String adminEmail = userDetails != null ? userDetails.getUsername() : "admin@bmart.com";
        Product product = adminService.createProduct(adminEmail, dto);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully and active on marketplace", product));
    }

    @PutMapping("/products/{id}/stock")
    public ResponseEntity<ApiResponse<Product>> updateProductStock(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body
    ) {
        Integer stock = body != null ? body.get("stock") : 0;
        if (stock == null || stock < 0) stock = 0;
        String adminEmail = userDetails != null ? userDetails.getUsername() : "admin@bmart.com";
        Product product = adminService.updateProductStock(adminEmail, id, stock);
        return ResponseEntity.ok(ApiResponse.success("Product stock updated to " + stock + " units", product));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean confirm
    ) {
        String adminEmail = userDetails != null ? userDetails.getUsername() : "admin@bmart.com";
        adminService.deleteProduct(adminEmail, id, confirm);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", "Deleted product ID: " + id));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody com.bmart.dto.AdminUserUpdateRequest request
    ) {
        String adminEmail = userDetails != null ? userDetails.getUsername() : "admin@bmart.com";
        User user = adminService.updateUser(adminEmail, id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        String adminEmail = userDetails != null ? userDetails.getUsername() : "admin@bmart.com";
        adminService.deleteUser(adminEmail, id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", "Deleted user ID: " + id));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<Category>> createCategory(@RequestBody Category category) {
        Category saved = adminService.createCategory(category);
        return ResponseEntity.ok(ApiResponse.success("Category created successfully", saved));
    }

    // Phase 10 & 11: Orders, Refunds, Payouts
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<Order>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size
    ) {
        Page<Order> orders = adminService.getAllOrders(PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.success("Platform orders list", orders));
    }

    @PutMapping("/orders/{id}/refund")
    public ResponseEntity<ApiResponse<Order>> processRefund(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.get("reason") : "Refund approved by admin";
        Order order = adminService.processRefund(userDetails.getUsername(), id, reason);
        return ResponseEntity.ok(ApiResponse.success("Order marked as REFUNDED", order));
    }

    @GetMapping("/payouts/pending")
    public ResponseEntity<ApiResponse<List<PayoutRequest>>> getPendingPayouts() {
        return ResponseEntity.ok(ApiResponse.success("Pending seller payout requests", adminService.getPendingPayouts()));
    }

    @PutMapping("/payouts/{id}/approve")
    public ResponseEntity<ApiResponse<PayoutRequest>> approvePayout(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String note = body != null ? body.get("note") : "Approved";
        PayoutRequest payout = adminService.approvePayout(userDetails.getUsername(), id, note);
        return ResponseEntity.ok(ApiResponse.success("Payout request approved", payout));
    }

    @PutMapping("/sellers/{id}/commission")
    public ResponseEntity<ApiResponse<Seller>> setSellerCommission(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> body
    ) {
        BigDecimal rate = body.get("commissionRate");
        if (rate == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("commissionRate field is required"));
        }
        Seller seller = adminService.setSellerCommission(userDetails.getUsername(), id, rate);
        return ResponseEntity.ok(ApiResponse.success("Seller commission rate updated to " + rate + "%", seller));
    }

    // Phase 13: Site Analytics
    @GetMapping("/analytics/site")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSiteAnalytics() {
        return ResponseEntity.ok(ApiResponse.success("Platform site analytics", adminService.getSiteAnalytics()));
    }
}
