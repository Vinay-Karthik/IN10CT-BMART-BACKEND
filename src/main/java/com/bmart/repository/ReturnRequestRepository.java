package com.bmart.repository;

import com.bmart.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findBySellerSellerId(Long sellerId);
    List<ReturnRequest> findByStatus(String status);
}
