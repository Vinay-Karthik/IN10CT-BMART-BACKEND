package com.bmart.repository;

import com.bmart.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductProductIdOrderByCreatedAtDesc(Long productId);
    List<Review> findByProductSellerSellerIdOrderByCreatedAtDesc(Long sellerId);
    List<Review> findByStatus(String status);
}
