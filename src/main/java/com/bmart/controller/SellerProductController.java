package com.bmart.controller;

import com.bmart.dto.ApiResponse;
import com.bmart.dto.SellerProductRequestDTO;
import com.bmart.entity.Product;
import com.bmart.service.SellerProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/products")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SELLER') or hasRole('SELLER') or hasAuthority('ROLE_ADMIN') or hasRole('ADMIN')")
public class SellerProductController {

    private final SellerProductService sellerProductService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getMyProducts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status
    ) {
        List<Product> products = sellerProductService.getSellerProducts(userDetails.getUsername(), status);
        return ResponseEntity.ok(ApiResponse.success("Seller products retrieved", products));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> createProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SellerProductRequestDTO dto
    ) {
        Product product = sellerProductService.createProduct(userDetails.getUsername(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully and submitted for admin review", product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody SellerProductRequestDTO dto
    ) {
        try {
            Product product = sellerProductService.updateProduct(userDetails.getUsername(), id, dto);
            return ResponseEntity.ok(ApiResponse.success("Product updated successfully", product));
        } catch (RuntimeException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("403_FORBIDDEN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You do not own this product"));
            }
            throw ex;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        try {
            sellerProductService.deleteProduct(userDetails.getUsername(), id);
            return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
        } catch (RuntimeException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("403_FORBIDDEN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You do not own this product"));
            }
            throw ex;
        }
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<Product>> updateStock(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body
    ) {
        Integer stock = body.get("stock");
        if (stock == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Stock field is required"));
        }
        Product product = sellerProductService.updateStock(userDetails.getUsername(), id, stock);
        return ResponseEntity.ok(ApiResponse.success("Stock updated successfully", product));
    }

    @PutMapping("/{id}/price")
    public ResponseEntity<ApiResponse<Product>> updatePrice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> body
    ) {
        BigDecimal price = body.get("price");
        BigDecimal discountPrice = body.get("discountPrice");
        Product product = sellerProductService.updatePrice(userDetails.getUsername(), id, price, discountPrice);
        return ResponseEntity.ok(ApiResponse.success("Price updated successfully", product));
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<ApiResponse<Product>> uploadImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String imageUrl = body.get("imageUrl");
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("imageUrl field is required"));
        }
        Product product = sellerProductService.updateImage(userDetails.getUsername(), id, imageUrl.trim());
        return ResponseEntity.ok(ApiResponse.success("Product image attached successfully", product));
    }
}
