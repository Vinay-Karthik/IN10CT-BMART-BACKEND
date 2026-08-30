package com.bmart.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public String generateReply(String systemPrompt, String userMessage) {
        if (geminiApiKey == null || geminiApiKey.isBlank() || geminiApiKey.contains("your_gemini_key")) {
            return null;
        }

        try {
            String fullPrompt = systemPrompt + "\n\nUser Question: " + userMessage;
            String escapedPrompt = fullPrompt.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");

            String requestBody = String.format(
                    "{\"contents\": [{\"parts\": [{\"text\": \"%s\"}]}]}",
                    escapedPrompt
            );

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null) {
                String body = response.body();
                int textIdx = body.indexOf("\"text\": \"");
                if (textIdx != -1) {
                    int start = textIdx + 9;
                    int end = body.indexOf("\"", start);
                    if (end != -1) {
                        String rawText = body.substring(start, end);
                        return rawText.replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Gemini API call warning: " + e.getMessage());
        }
        return null;
    }
}
