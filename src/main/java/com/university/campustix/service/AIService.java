package com.university.campustix.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AIService {

    @Value("${ai.base-url:https://generativelanguage.googleapis.com/v1beta/openai}")
    private String baseUrl;

    @Value("${ai.api-key:AIzaSyA0XcH4MfNHdZxIdXIRpSomSPu5aslkR_Y}")
    private String apiKey;

    @Value("${ai.model:gemini-2.0-flash}")
    private String model;

    private final RestClient restClient = RestClient.create();

    public String generateEventDescription(String eventName, String venue, String category, Double price) {
        String prompt = String.format(
            "Write a compelling 2-3 sentence event description for a ticketing platform. " +
            "Event: '%s' at %s. Category: %s. Ticket price: $%.2f. " +
            "Make it exciting, concise, and professional. Return only the description text.",
            eventName, venue, category != null ? category : "Event", price != null ? price : 0.0
        );
        return callLLM(prompt);
    }

    public String chat(String eventName, String venue, String eventTime, String description, String userMessage) {
        String prompt = String.format(
            "You are a helpful assistant for CampusTix, an event ticketing platform. " +
            "Answer questions about this specific event only.\n\n" +
            "Event: %s\nVenue: %s\nDate/Time: %s\nDescription: %s\n\n" +
            "User question: %s\n\n" +
            "Be concise (2-3 sentences max). If the question is unrelated to this event, politely redirect.",
            eventName, venue, eventTime,
            description != null ? description : "No description available.",
            userMessage
        );
        return callLLM(prompt);
    }

    private String callLLM(String prompt) {
        // OpenAI-compatible request format (used by Unsloth Studio / llama.cpp servers)
        var requestBody = Map.of(
            "model", model,
            "max_tokens", 512,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + "/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            // OpenAI-compatible response: choices[0].message.content
            @SuppressWarnings("unchecked")
            var choices = (List<Map<String, Object>>) response.get("choices");
            @SuppressWarnings("unchecked")
            var message = (Map<String, Object>) choices.get(0).get("message");
            return message.get("content").toString().trim();

        } catch (Exception e) {
            return "AI assistant unavailable. Set AI_API_KEY to your Gemini API key (free at aistudio.google.com).";
        }
    }
}
