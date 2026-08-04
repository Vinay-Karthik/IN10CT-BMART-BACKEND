package com.bmart.service;

import com.bmart.dto.OrderRequest;
import com.bmart.entity.*;
import com.bmart.repository.OrderRepository;
import com.bmart.repository.UserRepository;
import com.bmart.repository.PaymentRepository;
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

    public Order createOrderFromCart(String email, OrderRequest request) {
        User user = userRepository.findByEmail(email)
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
            BigDecimal itemTotal = cartItem.getProduct().getPrice().multiply(new BigDecimal(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .pricePerUnit(cartItem.getProduct().getPrice())
                    .totalPrice(itemTotal)
                    .build();

            orderItems.add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        orderRepository.save(order);

        String paymentMode = request.getPaymentMode() != null ? request.getPaymentMode() : "RAZORPAY";

        if ("COD".equalsIgnoreCase(paymentMode)) {
            order.setStatus("CONFIRMED");
            orderRepository.save(order);

            Payment payment = Payment.builder()
                    .order(order)
                    .razorpayOrderId("COD-" + System.currentTimeMillis())
                    .amount(order.getTotalAmount())
                    .status("SUCCESS")
                    .paymentMode("COD")
                    .build();
            paymentRepository.save(payment);

            cartService.clearCart(email);
        } else {
            // Generate Razorpay Order ID
            paymentService.createRazorpayOrder(order);
        }

        // Send notification
        notificationService.createNotification(user, "Order Placed Successfully", "Your order #" + order.getOrderId() + " has been placed.");

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
