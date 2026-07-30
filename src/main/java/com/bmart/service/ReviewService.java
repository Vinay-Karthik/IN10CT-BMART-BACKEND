package com.bmart.service;

import com.bmart.dto.ReviewRequest;
import com.bmart.entity.Product;
import com.bmart.entity.Review;
import com.bmart.entity.User;
import com.bmart.repository.ProductRepository;
import com.bmart.repository.ReviewRepository;
import com.bmart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public Review addReview(String email, ReviewRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);

        // Recalculate average rating & review count for product
        List<Review> productReviews = reviewRepository.findByProductProductIdOrderByCreatedAtDesc(product.getProductId());
        double avgRating = productReviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(product.getRating());

        product.setRating(Math.round(avgRating * 10.0) / 10.0);
        product.setReviewCount(productReviews.size());
        productRepository.save(product);

        return saved;
    }

    public List<Review> getProductReviews(Long productId) {
        return reviewRepository.findByProductProductIdOrderByCreatedAtDesc(productId);
    }
}
