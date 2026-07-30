package com.bmart.service;

import com.bmart.entity.Product;
import com.bmart.entity.User;
import com.bmart.entity.Wishlist;
import com.bmart.repository.ProductRepository;
import com.bmart.repository.UserRepository;
import com.bmart.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public List<Wishlist> getUserWishlist(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return wishlistRepository.findByUserUserIdOrderByCreatedAtDesc(user.getUserId());
    }

    public boolean toggleWishlist(String email, Long productId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return wishlistRepository.findByUserUserIdAndProductProductId(user.getUserId(), productId)
                .map(item -> {
                    wishlistRepository.delete(item);
                    return false; // Removed
                })
                .orElseGet(() -> {
                    Wishlist newItem = Wishlist.builder()
                            .user(user)
                            .product(product)
                            .build();
                    wishlistRepository.save(newItem);
                    return true; // Added
                });
    }

    public boolean isInWishlist(String email, Long productId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return wishlistRepository.existsByUserUserIdAndProductProductId(user.getUserId(), productId);
    }
}
