package com.bmart.controller;

import com.bmart.dto.ApiResponse;
import com.bmart.dto.ChatbotRequest;
import com.bmart.dto.ChatbotResponse;
import com.bmart.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/query")
    public ResponseEntity<ApiResponse<ChatbotResponse>> query(
            @Valid @RequestBody ChatbotRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        ChatbotResponse response = chatbotService.processQuery(request, username);
        return ResponseEntity.ok(ApiResponse.success("Query processed successfully", response));
    }
}
