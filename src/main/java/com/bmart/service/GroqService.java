package com.bmart.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class GroqService {

    @Value("${groq.api.key:dummy_groq_key}")
    private String groqApiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String generateChatReply(String systemPrompt, String userMessage) {
        if (groqApiKey == null || groqApiKey.isBlank() || groqApiKey.contains("your_groq_api_key")) {
            System.out.println("Groq API Key missing or default, using fallback.");
            return null;
        }

        try {
            String sanitizedSystem = systemPrompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
            String sanitizedUser = userMessage.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");

            String requestBody = String.format(
                    "{" +
                    "\"model\":\"%s\"," +
                    "\"messages\":[" +
                    "{\"role\":\"system\",\"content\":\"%s\"}," +
                    "{\"role\":\"user\",\"content\":\"%s\"}" +
                    "]," +
                    "\"temperature\":0.7," +
                    "\"max_tokens\":600" +
                    "}",
                    groqModel,
                    sanitizedSystem,
                    sanitizedUser
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + groqApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(12))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null) {
                String body = response.body();
                int contentIdx = body.indexOf("\"content\": \"");
                if (contentIdx != -1) {
                    int start = contentIdx + 12;
                    int end = body.indexOf("\"", start);
                    // Account for escaped quotes
                    while (end != -1 && body.charAt(end - 1) == '\\') {
                        end = body.indexOf("\"", end + 1);
                    }
                    if (end != -1) {
                        String rawContent = body.substring(start, end);
                        return rawContent.replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                    }
                }
            } else {
                System.err.println("Groq API response status: " + response.statusCode() + " body: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Groq API execution error: " + e.getMessage());
        }
        return null;
    }
}
