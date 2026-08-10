package com.bmart.service;

import com.bmart.dto.SellerProductRequestDTO;
import com.bmart.entity.Category;
import com.bmart.entity.Product;
import com.bmart.entity.Seller;
import com.bmart.entity.User;
import com.bmart.repository.CategoryRepository;
import com.bmart.repository.ProductRepository;
import com.bmart.repository.SellerRepository;
import com.bmart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerProductService {

    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private Seller getAuthenticatedSeller(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return sellerRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User is not registered as a seller"));
    }

    private Product getProductBelongingToSeller(Long productId, Seller seller) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));
        if (product.getSeller() == null || !product.getSeller().getSellerId().equals(seller.getSellerId())) {
            throw new RuntimeException("403_FORBIDDEN: You do not own this product");
        }
        return product;
    }

    public List<Product> getSellerProducts(String email, String status) {
        Seller seller = getAuthenticatedSeller(email);
        if (status != null && !status.trim().isEmpty()) {
            return productRepository.findBySellerSellerIdAndStatus(seller.getSellerId(), status.trim().toUpperCase());
        }
        return productRepository.findBySellerSellerId(seller.getSellerId());
    }

    @Transactional
    public Product createProduct(String email, SellerProductRequestDTO dto) {
        Seller seller = getAuthenticatedSeller(email);
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + dto.getCategoryId()));

        Product product = Product.builder()
                .seller(seller)
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .discountPrice(dto.getDiscountPrice())
                .brand(dto.getBrand() != null ? dto.getBrand().trim() : seller.getStoreName())
                .tags(dto.getTags())
                .stock(dto.getStock())
                .category(category)
                .imageUrl(dto.getImageUrl())
                .status("PENDING") // Defaults to PENDING for admin approval
                .rating(4.5)
                .reviewCount(0)
                .build();

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(String email, Long productId, SellerProductRequestDTO dto) {
        Seller seller = getAuthenticatedSeller(email);
        Product product = getProductBelongingToSeller(productId, seller);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + dto.getCategoryId()));

        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setDiscountPrice(dto.getDiscountPrice());
        if (dto.getBrand() != null)
            product.setBrand(dto.getBrand().trim());
        product.setTags(dto.getTags());
        product.setStock(dto.getStock());
        product.setCategory(category);
        product.setImageUrl(dto.getImageUrl());
        product.setStatus("PENDING"); // Re-trigger pending state on major edits

        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(String email, Long productId) {
        Seller seller = getAuthenticatedSeller(email);
        Product product = getProductBelongingToSeller(productId, seller);
        product.setStatus("DELETED"); // Soft delete
        productRepository.save(product);
    }

    @Transactional
    public Product updateStock(String email, Long productId, Integer stock) {
        Seller seller = getAuthenticatedSeller(email);
        Product product = getProductBelongingToSeller(productId, seller);
        if (stock < 0)
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        product.setStock(stock);
        return productRepository.save(product);
    }

    @Transactional
    public Product updatePrice(String email, Long productId, BigDecimal price, BigDecimal discountPrice) {
        Seller seller = getAuthenticatedSeller(email);
        Product product = getProductBelongingToSeller(productId, seller);

        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            product.setPrice(price);
        }
        if (discountPrice != null && discountPrice.compareTo(BigDecimal.ZERO) >= 0) {
            if (discountPrice.compareTo(product.getPrice()) >= 0) {
                throw new IllegalArgumentException("Discount price must be less than regular price");
            }
            product.setDiscountPrice(discountPrice);
        }
        return productRepository.save(product);
    }

    @Transactional
    public Product updateImage(String email, Long productId, String imageUrl) {
        Seller seller = getAuthenticatedSeller(email);
        Product product = getProductBelongingToSeller(productId, seller);
        product.setImageUrl(imageUrl);
        return productRepository.save(product);
    }
}
