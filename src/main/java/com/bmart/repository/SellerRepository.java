package com.bmart.repository;

import com.bmart.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {
    Optional<Seller> findByUserUserId(Long userId);
    Optional<Seller> findByUserEmail(String email);
    Optional<Seller> findByStoreSlug(String storeSlug);
    Boolean existsByStoreSlug(String storeSlug);
    List<Seller> findByStatus(String status);
}
