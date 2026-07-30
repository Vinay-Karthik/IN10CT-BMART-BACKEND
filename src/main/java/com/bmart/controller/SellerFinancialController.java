package com.bmart.controller;

import com.bmart.dto.ApiResponse;
import com.bmart.entity.PayoutRequest;
import com.bmart.service.SellerFinancialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SELLER') or hasRole('SELLER') or hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
public class SellerFinancialController {

    private final SellerFinancialService sellerFinancialService;

    @GetMapping("/earnings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEarningsOverview(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> earnings = sellerFinancialService.getEarningsOverview(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Earnings overview retrieved", earnings));
    }

    @PostMapping("/payouts/request")
    public ResponseEntity<ApiResponse<PayoutRequest>> requestPayout(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body
    ) {
        String amountStr = body.get("amount");
        String bankDetails = body.get("bankDetails");

        if (amountStr == null || amountStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Amount field is required"));
        }

        try {
            BigDecimal amount = new BigDecimal(amountStr.trim());
            PayoutRequest request = sellerFinancialService.requestPayout(userDetails.getUsername(), amount, bankDetails);
            return ResponseEntity.ok(ApiResponse.success("Payout request submitted successfully. Pending admin approval.", request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/payouts")
    public ResponseEntity<ApiResponse<List<PayoutRequest>>> getPayoutHistory(@AuthenticationPrincipal UserDetails userDetails) {
        List<PayoutRequest> history = sellerFinancialService.getPayoutHistory(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Payout request history retrieved", history));
    }
}
