package com.bmart.service;

import com.bmart.entity.OrderItem;
import com.bmart.entity.PayoutRequest;
import com.bmart.entity.Seller;
import com.bmart.entity.User;
import com.bmart.repository.OrderItemRepository;
import com.bmart.repository.PayoutRequestRepository;
import com.bmart.repository.SellerRepository;
import com.bmart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SellerFinancialService {

    private final OrderItemRepository orderItemRepository;
    private final PayoutRequestRepository payoutRequestRepository;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;

    private Seller getAuthenticatedSeller(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return sellerRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User is not registered as a seller"));
    }

    public Map<String, Object> getEarningsOverview(String email) {
        Seller seller = getAuthenticatedSeller(email);
        List<OrderItem> items = orderItemRepository.findBySellerId(seller.getSellerId());

        BigDecimal grossSales = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal netEarnings = BigDecimal.ZERO;

        BigDecimal commissionRate = seller.getCommissionRate() != null 
                ? seller.getCommissionRate() 
                : new BigDecimal("10.00"); // 10% platform default commission

        for (OrderItem item : items) {
            if ("DELIVERED".equalsIgnoreCase(item.getStatus()) || "SHIPPED".equalsIgnoreCase(item.getStatus())) {
                BigDecimal itemTotal = item.getTotalPrice();
                grossSales = grossSales.add(itemTotal);

                BigDecimal commission = itemTotal.multiply(commissionRate)
                        .divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);
                totalCommission = totalCommission.add(commission);
                netEarnings = netEarnings.add(itemTotal.subtract(commission));
            }
        }

        List<PayoutRequest> payouts = payoutRequestRepository.findBySellerSellerId(seller.getSellerId());
        BigDecimal totalWithdrawn = payouts.stream()
                .filter(p -> "APPROVED".equalsIgnoreCase(p.getStatus()))
                .map(PayoutRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableBalance = netEarnings.subtract(totalWithdrawn);
        if (availableBalance.compareTo(BigDecimal.ZERO) < 0) availableBalance = BigDecimal.ZERO;

        Map<String, Object> response = new HashMap<>();
        response.put("grossSales", grossSales);
        response.put("commissionRate", commissionRate + "%");
        response.put("totalCommissionDeducted", totalCommission);
        response.put("netEarnings", netEarnings);
        response.put("totalWithdrawn", totalWithdrawn);
        response.put("availableBalance", availableBalance);
        return response;
    }

    @Transactional
    public PayoutRequest requestPayout(String email, BigDecimal amount, String bankDetails) {
        Seller seller = getAuthenticatedSeller(email);

        Map<String, Object> overview = getEarningsOverview(email);
        BigDecimal availableBalance = (BigDecimal) overview.get("availableBalance");

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payout amount must be greater than 0");
        }
        if (amount.compareTo(availableBalance) > 0) {
            throw new IllegalArgumentException("Requested payout amount (₹" + amount + ") exceeds available balance (₹" + availableBalance + ")");
        }

        PayoutRequest payoutRequest = PayoutRequest.builder()
                .seller(seller)
                .amount(amount)
                .bankDetails(bankDetails)
                .status("PENDING")
                .build();

        return payoutRequestRepository.save(payoutRequest);
    }

    public List<PayoutRequest> getPayoutHistory(String email) {
        Seller seller = getAuthenticatedSeller(email);
        return payoutRequestRepository.findBySellerSellerId(seller.getSellerId());
    }
}
