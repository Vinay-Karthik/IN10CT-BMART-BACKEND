package com.bmart.repository;

import com.bmart.entity.PayoutRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, Long> {
    List<PayoutRequest> findBySellerSellerId(Long sellerId);
    List<PayoutRequest> findByStatus(String status);
}
