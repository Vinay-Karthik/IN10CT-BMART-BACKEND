package com.bmart.service;

import com.bmart.dto.OrderRequest;
import com.bmart.entity.*;
import com.bmart.repository.OrderRepository;
import com.bmart.repository.UserRepository;
import com.bmart.repository.PaymentRepository;
import com.bmart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;

    @org.springframework.transaction.annotation.Transactional
    public Order createOrderFromCart(String email, OrderRequest request) {
        User user = userRepository.findByEmailOrUsername(email, email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartService.getOrCreateCart(email);
        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        String orderId = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        Order order = Order.builder()
                .orderId(orderId)
                .user(user)
                .shippingAddress(request.getShippingAddress())
                .status("PLACED")
                .totalAmount(BigDecimal.ZERO)
                .build();

        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            int purchasedQty = cartItem.getQuantity();

            if (product != null) {
                int currentStock = product.getStock() != null ? product.getStock() : 0;
                int updatedStock = Math.max(0, currentStock - purchasedQty);
                product.setStock(updatedStock);
                productRepository.save(product);
            }

            BigDecimal itemTotal = (product != null && product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO)
                    .multiply(new BigDecimal(purchasedQty));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(purchasedQty)
                    .pricePerUnit(product != null ? product.getPrice() : BigDecimal.ZERO)
                    .totalPrice(itemTotal)
                    .build();

            orderItems.add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        String paymentMode = request.getPaymentMode() != null ? request.getPaymentMode().toUpperCase() : "RAZORPAY";
        if ("COD".equalsIgnoreCase(paymentMode)) {
            // Add COD Handling & Delivery Fee of ₹49
            totalAmount = totalAmount.add(new BigDecimal("49.00"));
            order.setTotalAmount(totalAmount);
            order.setStatus("CONFIRMED");
            order.setPaymentStatus("SUCCESS");
            orderRepository.save(order);
            paymentService.createCodPayment(order);
            cartService.clearCart(email);
            // Send notification for COD
            notificationService.createNotification(user, "Order Placed Successfully", "Your order #" + order.getOrderId() + " has been placed (COD).");
        } else {
            order.setTotalAmount(totalAmount);
            order.setStatus("PENDING_PAYMENT");
            order.setPaymentStatus("PENDING");
            orderRepository.save(order);
            paymentService.createRazorpayOrder(order);
        }

        return order;
    }

    public List<Order> getUserOrders(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByUserUserIdOrderByCreatedAtDesc(user.getUserId());
    }

    public Order getOrderById(String orderId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (!order.getUser().getUserId().equals(user.getUserId()) && !"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Unauthorized order access");
        }

        return order;
    }

    public Order updateOrderStatus(String orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        order.setStatus(status);
        orderRepository.save(order);

        notificationService.createNotification(order.getUser(), "Order Status Updated", "Your order #" + orderId + " is now " + status);

        return order;
    }
}
