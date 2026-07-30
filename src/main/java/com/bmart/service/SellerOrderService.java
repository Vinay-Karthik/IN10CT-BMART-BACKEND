package com.bmart.service;

import com.bmart.entity.OrderItem;
import com.bmart.entity.ReturnRequest;
import com.bmart.entity.Seller;
import com.bmart.entity.User;
import com.bmart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SellerOrderService {

    private final OrderItemRepository orderItemRepository;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final ReturnRequestRepository returnRequestRepository;

    private Seller getAuthenticatedSeller(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return sellerRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User is not registered as a seller"));
    }

    public List<OrderItem> getSellerOrderItems(String email) {
        Seller seller = getAuthenticatedSeller(email);
        return orderItemRepository.findBySellerId(seller.getSellerId());
    }

    @Transactional
    public OrderItem updateOrderStatus(String email, Long orderItemId, String newStatusStr) {
        Seller seller = getAuthenticatedSeller(email);
        OrderItem item = orderItemRepository.findByIdAndSellerId(orderItemId, seller.getSellerId())
                .orElseThrow(() -> new RuntimeException("Order item not found or does not belong to seller"));

        String currentStatus = item.getStatus().toUpperCase();
        String targetStatus = newStatusStr.trim().toUpperCase();

        // Legal status transition validation rules
        if (currentStatus.equals("PLACED")) {
            if (!targetStatus.equals("PROCESSING") && !targetStatus.equals("CANCELLED")) {
                throw new IllegalArgumentException("Cannot transition order from PLACED directly to " + targetStatus + ". Allowed next status: PROCESSING or CANCELLED.");
            }
        } else if (currentStatus.equals("PROCESSING")) {
            if (!targetStatus.equals("SHIPPED") && !targetStatus.equals("CANCELLED")) {
                throw new IllegalArgumentException("Cannot transition order from PROCESSING directly to " + targetStatus + ". Allowed next status: SHIPPED or CANCELLED.");
            }
        } else if (currentStatus.equals("SHIPPED")) {
            if (!targetStatus.equals("DELIVERED") && !targetStatus.equals("RETURNED")) {
                throw new IllegalArgumentException("Cannot transition order from SHIPPED directly to " + targetStatus + ". Allowed next status: DELIVERED or RETURNED.");
            }
        } else if (currentStatus.equals("DELIVERED") || currentStatus.equals("CANCELLED")) {
            throw new IllegalArgumentException("Order item is already in terminal status: " + currentStatus);
        }

        item.setStatus(targetStatus);
        return orderItemRepository.save(item);
    }

    @Transactional
    public ReturnRequest initiateReturn(String email, Long orderItemId, String reason) {
        Seller seller = getAuthenticatedSeller(email);
        OrderItem item = orderItemRepository.findByIdAndSellerId(orderItemId, seller.getSellerId())
                .orElseThrow(() -> new RuntimeException("Order item not found or does not belong to seller"));

        ReturnRequest request = ReturnRequest.builder()
                .orderItemId(orderItemId)
                .seller(seller)
                .user(item.getOrder().getUser())
                .reason(reason != null ? reason : "Seller initiated return")
                .status("PENDING") // Requires admin approval
                .build();

        return returnRequestRepository.save(request);
    }

    public Map<String, Object> generateInvoice(String email, Long orderItemId) {
        Seller seller = getAuthenticatedSeller(email);
        OrderItem item = orderItemRepository.findByIdAndSellerId(orderItemId, seller.getSellerId())
                .orElseThrow(() -> new RuntimeException("Order item not found or does not belong to seller"));

        Map<String, Object> invoice = new HashMap<>();
        invoice.put("invoiceNumber", "INV-" + item.getOrder().getOrderId() + "-" + item.getId());
        invoice.put("orderId", item.getOrder().getOrderId());
        invoice.put("sellerStoreName", seller.getStoreName());
        invoice.put("customerName", item.getOrder().getUser().getFullName());
        invoice.put("customerEmail", item.getOrder().getUser().getEmail());
        invoice.put("shippingAddress", item.getOrder().getShippingAddress());
        invoice.put("productName", item.getProduct().getName());
        invoice.put("quantity", item.getQuantity());
        invoice.put("pricePerUnit", item.getPricePerUnit());
        invoice.put("totalPrice", item.getTotalPrice());
        invoice.put("status", item.getStatus());
        invoice.put("generatedAt", new Date().toString());

        return invoice;
    }
}
