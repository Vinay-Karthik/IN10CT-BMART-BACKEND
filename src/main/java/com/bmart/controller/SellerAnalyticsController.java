package com.bmart.controller;

import com.bmart.dto.ApiResponse;
import com.bmart.service.SellerAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SELLER') or hasRole('SELLER') or hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
public class SellerAnalyticsController {

    private final SellerAnalyticsService sellerAnalyticsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalyticsOverview(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> overview = sellerAnalyticsService.getAnalyticsOverview(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Analytics overview retrieved", overview));
    }

    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopProducts(@AuthenticationPrincipal UserDetails userDetails) {
        List<Map<String, Object>> topProducts = sellerAnalyticsService.getTopProducts(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Top selling products retrieved", topProducts));
    }
}
