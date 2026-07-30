package com.bmart.controller;

import com.bmart.dto.ApiResponse;
import com.bmart.entity.Wishlist;
import com.bmart.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Wishlist>>> getUserWishlist(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Wishlist retrieved", wishlistService.getUserWishlist(userDetails.getUsername())));
    }

    @PostMapping("/toggle/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> toggleWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId
    ) {
        boolean isAdded = wishlistService.toggleWishlist(userDetails.getUsername(), productId);
        String msg = isAdded ? "Added to wishlist" : "Removed from wishlist";
        return ResponseEntity.ok(ApiResponse.success(msg, isAdded));
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> checkWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Wishlist status retrieved", wishlistService.isInWishlist(userDetails.getUsername(), productId)));
    }
}
