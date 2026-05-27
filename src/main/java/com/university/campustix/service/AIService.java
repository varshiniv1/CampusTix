package com.university.campustix.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AIService {

    @Value("${ai.api.key:}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

    public String generateEventDescription(String eventName, String venue, String category, Double price) {
        String prompt = String.format(
            "Write a compelling 2-3 sentence event description for a ticketing platform. " +
            "Event: '%s' at %s. Category: %s. Ticket price: $%.2f. " +
            "Make it exciting, concise, and professional. Return only the description text.",
            eventName, venue, category != null ? category : "Event", price != null ? price : 0.0
        );
        return callClaude(prompt);
    }

    public String chat(String eventName, String venue, String eventTime, String description, String userMessage) {
        String prompt = String.format(
            "You are a helpful assistant for CampusTix, an event ticketing platform. " +
            "Answer questions about this specific event only.\n\n" +
            "Event: %s\nVenue: %s\nDate/Time: %s\nDescription: %s\n\n" +
            "User question: %s\n\n" +
            "Be concise (2-3 sentences max). If the question is unrelated to this event, politely redirect.",
            eventName, venue, eventTime, description != null ? description : "No description available.", userMessage
        );
        return callClaude(prompt);
    }

    private String callClaude(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return "AI features are not configured. Add your Anthropic API key to application.yml under ai.api.key.";
        }

        var requestBody = Map.of(
            "model", "claude-haiku-4-5-20251001",
            "max_tokens", 512,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("https://api.anthropic.com/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            @SuppressWarnings("unchecked")
            var content = (List<Map<String, Object>>) response.get("content");
            return content.get(0).get("text").toString();
        } catch (Exception e) {
            return "AI assistant is temporarily unavailable. Please try again later.";
        }
    }
}
