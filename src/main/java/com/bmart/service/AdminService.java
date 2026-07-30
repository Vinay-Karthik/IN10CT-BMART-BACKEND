package com.bmart.service;

import com.bmart.entity.*;
import com.bmart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final PayoutRequestRepository payoutRequestRepository;
    private final AuditLogRepository auditLogRepository;

    // Phase 8: User & Seller Oversight
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public User suspendUser(String adminEmail, Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setStatus("SUSPENDED");
        User saved = userRepository.save(user);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("SUSPEND_USER")
                .targetType("USER")
                .targetId(String.valueOf(userId))
                .reason(reason != null ? reason : "Suspended by admin")
                .build());

        return saved;
    }

    @Transactional
    public User banUser(String adminEmail, Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setStatus("BANNED");
        User saved = userRepository.save(user);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("BAN_USER")
                .targetType("USER")
                .targetId(String.valueOf(userId))
                .reason(reason != null ? reason : "Banned by admin for policy violation")
                .build());

        return saved;
    }

    @Transactional
    public User changeUserRole(String adminEmail, Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.setRole(newRole.trim().toUpperCase());
        User saved = userRepository.save(user);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("CHANGE_USER_ROLE")
                .targetType("USER")
                .targetId(String.valueOf(userId))
                .reason("Promoted/Changed role to " + newRole)
                .build());

        return saved;
    }

    // Phase 9: Product Moderation & Categories
    public List<Product> getPendingProducts() {
        return productRepository.findByStatus("PENDING");
    }

    @Transactional
    public Product approveProduct(String adminEmail, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));

        product.setStatus("APPROVED");
        Product saved = productRepository.save(product);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("APPROVE_PRODUCT")
                .targetType("PRODUCT")
                .targetId(String.valueOf(productId))
                .reason("Approved product listing")
                .build());

        return saved;
    }

    @Transactional
    public Product banProduct(String adminEmail, Long productId, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));

        product.setStatus("BANNED");
        Product saved = productRepository.save(product);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("BAN_PRODUCT")
                .targetType("PRODUCT")
                .targetId(String.valueOf(productId))
                .reason(reason != null ? reason : "Banned for policy violation")
                .build());

        return saved;
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "categories", allEntries = true)
    public Category createCategory(Category category) {
        if (category.getSlug() == null || category.getSlug().trim().isEmpty()) {
            category.setSlug(category.getCategoryName().toLowerCase().replaceAll("[^a-z0-9]", "-"));
        }
        return categoryRepository.save(category);
    }

    // Phase 10 & 11: Orders, Financials, Payout Approvals
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    @Transactional
    public Order processRefund(String adminEmail, String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        order.setStatus("REFUNDED");
        Order saved = orderRepository.save(order);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("REFUND_ORDER")
                .targetType("ORDER")
                .targetId(orderId)
                .reason(reason != null ? reason : "Platform level refund approved")
                .build());

        return saved;
    }

    public List<PayoutRequest> getPendingPayouts() {
        return payoutRequestRepository.findByStatus("PENDING");
    }

    @Transactional
    public PayoutRequest approvePayout(String adminEmail, Long payoutId, String note) {
        PayoutRequest payout = payoutRequestRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout request not found with ID: " + payoutId));

        payout.setStatus("APPROVED");
        payout.setAdminNote(note != null ? note : "Payout approved and transferred");
        payout.setProcessedAt(LocalDateTime.now());

        PayoutRequest saved = payoutRequestRepository.save(payout);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("APPROVE_PAYOUT")
                .targetType("PAYOUT")
                .targetId(String.valueOf(payoutId))
                .reason("Approved withdrawal of ₹" + payout.getAmount() + " for seller " + payout.getSeller().getStoreName())
                .build());

        return saved;
    }

    @Transactional
    public Seller setSellerCommission(String adminEmail, Long sellerId, BigDecimal commissionRate) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found with ID: " + sellerId));

        seller.setCommissionRate(commissionRate);
        Seller saved = sellerRepository.save(seller);

        auditLogRepository.save(AuditLog.builder()
                .adminEmail(adminEmail)
                .action("SET_COMMISSION")
                .targetType("SELLER")
                .targetId(String.valueOf(sellerId))
                .reason("Updated commission rate to " + commissionRate + "%")
                .build());

        return saved;
    }

    // Phase 13: Site Analytics
    public Map<String, Object> getSiteAnalytics() {
        long totalUsers = userRepository.count();
        long totalSellers = sellerRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();

        Map<String, Object> site = new HashMap<>();
        site.put("totalUsers", totalUsers);
        site.put("totalSellers", totalSellers);
        site.put("totalProducts", totalProducts);
        site.put("totalOrders", totalOrders);
        return site;
    }
}
