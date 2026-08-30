package com.bmart.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Product description is required")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    private BigDecimal discountPrice;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    private Integer stock; // alias fallback for stockQuantity

    private Object category; // Can be Long (categoryId) or String (categoryName/slug)

    private Long categoryId;

    private String categoryName;

    private String brand;

    private String imageUrl;

    private String tags;

    public Integer getEffectiveStock() {
        if (stockQuantity != null) return stockQuantity;
        if (stock != null) return stock;
        return 0;
    }
}
