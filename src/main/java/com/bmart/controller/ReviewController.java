package com.bmart.controller;

import com.bmart.dto.ApiResponse;
import com.bmart.dto.ReviewRequest;
import com.bmart.entity.Review;
import com.bmart.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<Review>> addReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Review submitted successfully", reviewService.addReview(userDetails.getUsername(), request)));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<Review>>> getProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Product reviews retrieved", reviewService.getProductReviews(productId)));
    }
}
