package com.bmart.service;

import com.bmart.dto.SellerApplyRequestDTO;
import com.bmart.entity.AuditLog;
import com.bmart.entity.Seller;
import com.bmart.entity.User;
import com.bmart.repository.AuditLogRepository;
import com.bmart.repository.SellerRepository;
import com.bmart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    private String generateSlug(String name) {
        String base = name.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
        String slug = base;
        int count = 1;
        while (sellerRepository.existsByStoreSlug(slug)) {
            slug = base + "-" + count++;
        }
        return slug;
    }

    @Transactional
    public Seller applyForSeller(String email, SellerApplyRequestDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        if (sellerRepository.findByUserUserId(user.getUserId()).isPresent()) {
            throw new RuntimeException("Seller application already submitted for this account");
        }

        Seller seller = Seller.builder()
                .user(user)
                .storeName(dto.getStoreName().trim())
                .storeSlug(generateSlug(dto.getStoreName()))
                .storeDescription(dto.getStoreDescription())
                .logoUrl(dto.getLogoUrl())
                .bannerUrl(dto.getBannerUrl())
                .status("PENDING")
                .build();

        return sellerRepository.save(seller);
    }

    public Seller getMySellerProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return sellerRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("No seller profile found for this user"));
    }

    @Transactional
    public Seller updateStoreProfile(String email, Map<String, String> body) {
        Seller seller = getMySellerProfile(email);
        if (body.containsKey("storeDescription")) seller.setStoreDescription(body.get("storeDescription"));
        if (body.containsKey("logoUrl")) seller.setLogoUrl(body.get("logoUrl"));
        if (body.containsKey("bannerUrl")) seller.setBannerUrl(body.get("bannerUrl"));
        if (body.containsKey("storeSlug") && !body.get("storeSlug").trim().isEmpty()) {
            String newSlug = generateSlug(body.get("storeSlug"));
            seller.setStoreSlug(newSlug);
        }
        return sellerRepository.save(seller);
    }

    public Seller getPublicStoreBySlug(String slug) {
        return sellerRepository.findByStoreSlug(slug)
                .orElseThrow(() -> new RuntimeException("Store not found with slug: " + slug));
    }

    // Admin Seller Approval Operations
    public List<Seller> getPendingSellers() {
        return sellerRepository.findByStatus("PENDING");
    }

    @Transactional
    public Seller approveSeller(String adminEmail, Long sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found with ID: " + sellerId));

        seller.setStatus("APPROVED");

        // Promote user role to ROLE_SELLER
        User user = seller.getUser();
        user.setRole("ROLE_SELLER");
        userRepository.save(user);

        Seller saved = sellerRepository.save(seller);

        // Audit Log
        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("APPROVE_SELLER")
                .targetType("SELLER")
                .targetId(String.valueOf(sellerId))
                .reason("Approved seller account application for store " + seller.getStoreName())
                .build());

        return saved;
    }

    @Transactional
    public Seller rejectSeller(String adminEmail, Long sellerId, String reason) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found with ID: " + sellerId));

        seller.setStatus("REJECTED");
        Seller saved = sellerRepository.save(seller);

        // Audit Log
        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("REJECT_SELLER")
                .targetType("SELLER")
                .targetId(String.valueOf(sellerId))
                .reason(reason != null ? reason : "Application rejected by admin")
                .build());

        return saved;
    }
}
