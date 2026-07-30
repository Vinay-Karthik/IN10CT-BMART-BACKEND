package com.bmart.repository;

import com.bmart.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Wishlist> findByUserUserIdAndProductProductId(Long userId, Long productId);
    Boolean existsByUserUserIdAndProductProductId(Long userId, Long productId);
}
