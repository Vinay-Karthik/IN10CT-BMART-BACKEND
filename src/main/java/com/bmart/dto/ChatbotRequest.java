package com.bmart.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotRequest {

    @NotBlank(message = "Message cannot be empty")
    private String message;

    private String sessionId;
}
