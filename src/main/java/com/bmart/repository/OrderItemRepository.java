package com.bmart.repository;

import com.bmart.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findBySellerId(Long sellerId);
    Optional<OrderItem> findByIdAndSellerId(Long id, Long sellerId);
    List<OrderItem> findBySellerIdAndStatus(Long sellerId, String status);
}
