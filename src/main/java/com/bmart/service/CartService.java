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

    public Cart getOrCreateCart(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return cartRepository.findByUserUserId(user.getUserId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .cartItems(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    public Cart addToCart(String email, CartItemRequest request) {
        Cart cart = getOrCreateCart(email);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Optional<CartItem> existingItem = cartItemRepository.findByCartCartIdAndProductProductId(cart.getCartId(), product.getProductId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
            cart.getCartItems().add(newItem);
        }

        return cartRepository.findById(cart.getCartId()).orElse(cart);
    }

    public Cart updateCartItemQuantity(String email, Long itemId, Integer quantity) {
        Cart cart = getOrCreateCart(email);
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Unauthorized cart item access");
        }

        if (quantity <= 0) {
            cart.getCartItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        } else {
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        }

        return cart;
    }

    public Cart removeCartItem(String email, Long itemId) {
        Cart cart = getOrCreateCart(email);
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new RuntimeException("Unauthorized cart item access");
        }

        cart.getCartItems().remove(cartItem);
        cartItemRepository.delete(cartItem);
        return cart;
    }

    public void clearCart(String email) {
        Cart cart = getOrCreateCart(email);
        cartItemRepository.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();
    }
}
