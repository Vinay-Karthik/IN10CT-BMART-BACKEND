package com.bmart.service;

import com.bmart.dto.CartItemRequest;
import com.bmart.entity.Cart;
import com.bmart.entity.CartItem;
import com.bmart.entity.Product;
import com.bmart.entity.User;
import com.bmart.repository.CartItemRepository;
import com.bmart.repository.CartRepository;
import com.bmart.repository.ProductRepository;
import com.bmart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public Cart getOrCreateCart(String identifier) {
        User user = userRepository.findByEmailOrUsername(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("User not found: " + identifier));

        return cartRepository.findByUserUserId(user.getUserId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .cartItems(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    public Cart addToCart(String identifier, CartItemRequest request) {
        Cart cart = getOrCreateCart(identifier);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + request.getProductId()));

        if (product.getStock() != null && product.getStock() <= 0) {
            throw new RuntimeException("Product is out of stock");
        }

        int requestedQty = request.getQuantity() != null && request.getQuantity() > 0 ? request.getQuantity() : 1;

        Optional<CartItem> existingItem = cartItemRepository.findByCartCartIdAndProductProductId(cart.getCartId(), product.getProductId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + requestedQty;
            if (product.getStock() != null && newQuantity > product.getStock()) {
                throw new RuntimeException("Requested quantity exceeds available stock (" + product.getStock() + ")");
            }
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            if (product.getStock() != null && requestedQty > product.getStock()) {
                throw new RuntimeException("Requested quantity exceeds available stock (" + product.getStock() + ")");
            }
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(requestedQty)
                    .build();
            cartItemRepository.save(newItem);
            cart.getCartItems().add(newItem);
        }

        return cartRepository.findById(cart.getCartId()).orElse(cart);
    }

    public Cart updateCartItemQuantity(String identifier, Long itemId, Integer quantity) {
        Cart cart = getOrCreateCart(identifier);
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Unauthorized cart item access");
        }

        if (quantity <= 0) {
            cart.getCartItems().removeIf(item -> item.getId().equals(itemId));
            cartItemRepository.delete(cartItem);
        } else {
            if (cartItem.getProduct().getStock() != null && quantity > cartItem.getProduct().getStock()) {
                throw new RuntimeException("Requested quantity exceeds available stock (" + cartItem.getProduct().getStock() + ")");
            }
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        }

        return cartRepository.findById(cart.getCartId()).orElse(cart);
    }

    public Cart removeCartItem(String identifier, Long itemId) {
        Cart cart = getOrCreateCart(identifier);
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Unauthorized cart item access");
        }

        cart.getCartItems().removeIf(item -> item.getId().equals(itemId));
        cartItemRepository.delete(cartItem);
        return cartRepository.findById(cart.getCartId()).orElse(cart);
    }

    public void clearCart(String identifier) {
        Cart cart = getOrCreateCart(identifier);
        cartItemRepository.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();
        cartRepository.save(cart);
    }
}
