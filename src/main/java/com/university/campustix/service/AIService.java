package com.university.campustix.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Service
public class AIService {

    @Value("${ai.base-url:https://api.groq.com/openai/v1}")
    private String baseUrl;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:llama-3.1-8b-instant}")
    private String model;

    private final RestClient restClient = RestClient.create();

    // Circuit breaker: opens after 50% failures in a 5-call window, waits 30s before retrying
    private final CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(
        CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slidingWindowSize(5)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(2)
            .build()
    ).circuitBreaker("ai");

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
        Callable<String> callable = CircuitBreaker.decorateCallable(
            circuitBreaker, () -> callLLMInternal(prompt)
        );
        try {
            return callable.call();
        } catch (Exception e) {
            System.err.println("=== AI CALL FAILED [circuit: " + circuitBreaker.getState() + "] ===");
            System.err.println("Error: " + e.getMessage());
            if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
                return "AI temporarily unavailable (circuit open — too many recent failures). Try again in 30 seconds.";
            }
            return "AI unavailable: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private String callLLMInternal(String prompt) {
        var requestBody = Map.of(
            "model", model,
            "max_tokens", 512,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
            .uri(baseUrl + "/chat/completions")
            .header("Authorization", "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody)
            .retrieve()
            .body(Map.class);

        @SuppressWarnings("unchecked")
        var choices = (List<Map<String, Object>>) response.get("choices");
        @SuppressWarnings("unchecked")
        var message = (Map<String, Object>) choices.get(0).get("message");
        return message.get("content").toString().trim();
    }
}
