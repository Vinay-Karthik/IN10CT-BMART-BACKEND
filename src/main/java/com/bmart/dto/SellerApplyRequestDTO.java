package com.bmart.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerApplyRequestDTO {

    @NotBlank(message = "Store name is required")
    private String storeName;

    private String storeDescription;

    private String logoUrl;

    private String bannerUrl;
}
