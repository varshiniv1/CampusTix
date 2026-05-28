package com.university.campustix.controller;

import com.university.campustix.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Stripe-compatible mock payment processing")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Create a payment intent for a ticket purchase")
    @PostMapping("/create-intent")
    public ResponseEntity<PaymentService.PaymentIntent> createIntent(
            @RequestParam double amount,
            @RequestParam(required = false, defaultValue = "Event Ticket") String description) {
        return ResponseEntity.ok(paymentService.create(amount, description));
    }

    @Operation(summary = "Confirm a payment intent (mock: always succeeds for pi_ prefixed IDs)")
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirm(@RequestParam String paymentIntentId) {
        if (paymentService.confirm(paymentIntentId)) {
            return ResponseEntity.ok(Map.of(
                    "status", "succeeded",
                    "paymentIntentId", paymentIntentId
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "status", "failed",
                "error", "Payment could not be confirmed"
        ));
    }
}
