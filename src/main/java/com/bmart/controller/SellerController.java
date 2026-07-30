package com.bmart.controller;

import com.bmart.dto.ApiResponse;
import com.bmart.dto.SellerApplyRequestDTO;
import com.bmart.entity.Seller;
import com.bmart.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    // Apply for seller account (Any authenticated customer)
    @PostMapping("/api/seller/apply")
    public ResponseEntity<ApiResponse<Seller>> applyForSeller(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SellerApplyRequestDTO dto
    ) {
        Seller seller = sellerService.applyForSeller(userDetails.getUsername(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Seller application submitted successfully. Pending admin approval.", seller));
    }

    // Get own seller profile
    @GetMapping("/api/seller/profile")
    @PreAuthorize("hasAuthority('ROLE_SELLER') or hasRole('SELLER') or hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Seller>> getMySellerProfile(@AuthenticationPrincipal UserDetails userDetails) {
        Seller seller = sellerService.getMySellerProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Seller profile retrieved", seller));
    }

    // Update store details
    @PutMapping("/api/seller/store")
    @PreAuthorize("hasAuthority('ROLE_SELLER') or hasRole('SELLER') or hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Seller>> updateStoreProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body
    ) {
        Seller seller = sellerService.updateStoreProfile(userDetails.getUsername(), body);
        return ResponseEntity.ok(ApiResponse.success("Store details updated successfully", seller));
    }

    // Public storefront endpoint
    @GetMapping("/api/store/{slug}")
    public ResponseEntity<ApiResponse<Seller>> getPublicStoreBySlug(@PathVariable String slug) {
        Seller seller = sellerService.getPublicStoreBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Store details retrieved", seller));
    }

    // Admin: List pending seller applications
    @GetMapping("/api/admin/sellers/pending")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Seller>>> getPendingSellers() {
        return ResponseEntity.ok(ApiResponse.success("Pending seller applications", sellerService.getPendingSellers()));
    }

    // Admin: Approve seller application
    @PutMapping("/api/admin/sellers/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Seller>> approveSeller(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        Seller seller = sellerService.approveSeller(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Seller approved successfully", seller));
    }

    // Admin: Reject seller application
    @PutMapping("/api/admin/sellers/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Seller>> rejectSeller(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.get("reason") : "Application rejected by admin";
        Seller seller = sellerService.rejectSeller(userDetails.getUsername(), id, reason);
        return ResponseEntity.ok(ApiResponse.success("Seller application rejected", seller));
    }
}
