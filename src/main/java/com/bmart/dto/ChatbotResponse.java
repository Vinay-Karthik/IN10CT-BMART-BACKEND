package com.bmart.dto;

import com.bmart.entity.Order;
import com.bmart.entity.Product;
import com.bmart.entity.SupportTicket;
import lombok.*;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotResponse {

    private String reply;
    private String intent;
    private List<Product> products;
    private List<Order> orders;
    private List<Product> comparisonProducts;
    private SupportTicket ticket;
    private List<String> quickChips;
    private List<Map<String, String>> actions;
}
