package com.bmart.service;

import com.bmart.entity.OrderMessage;
import com.bmart.entity.Review;
import com.bmart.entity.Seller;
import com.bmart.entity.User;
import com.bmart.repository.OrderMessageRepository;
import com.bmart.repository.ReviewRepository;
import com.bmart.repository.SellerRepository;
import com.bmart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerInteractionService {

    private final ReviewRepository reviewRepository;
    private final OrderMessageRepository orderMessageRepository;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;

    private Seller getAuthenticatedSeller(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return sellerRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User is not registered as a seller"));
    }

    public List<Review> getSellerProductReviews(String email) {
        Seller seller = getAuthenticatedSeller(email);
        return reviewRepository.findByProductSellerSellerIdOrderByCreatedAtDesc(seller.getSellerId());
    }

    @Transactional
    public Review replyToReview(String email, Long reviewId, String replyComment) {
        Seller seller = getAuthenticatedSeller(email);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with ID: " + reviewId));

        if (review.getProduct().getSeller() == null || !review.getProduct().getSeller().getSellerId().equals(seller.getSellerId())) {
            throw new RuntimeException("403_FORBIDDEN: You do not own the product associated with this review");
        }

        review.setReplyComment(replyComment);
        return reviewRepository.save(review);
    }

    public List<OrderMessage> getOrderMessages(String orderId) {
        return orderMessageRepository.findByOrderIdOrderByTimestampAsc(orderId);
    }

    @Transactional
    public OrderMessage sendOrderMessage(String senderEmail, String orderId, String messageText) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + senderEmail));

        OrderMessage message = OrderMessage.builder()
                .orderId(orderId)
                .sender(sender)
                .message(messageText)
                .build();

        return orderMessageRepository.save(message);
    }
}
