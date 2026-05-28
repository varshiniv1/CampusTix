package com.university.campustix.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {

    public record PaymentIntent(
            String id,
            String clientSecret,
            String status,
            long amountCents,
            String currency,
            String mode
    ) {}

    public PaymentIntent create(double amount, String description) {
        String id = "pi_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String secret = id + "_secret_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        return new PaymentIntent(id, secret, "requires_confirmation",
                Math.round(amount * 100), "usd", "mock");
    }

    public boolean confirm(String paymentIntentId) {
        return paymentIntentId != null && paymentIntentId.startsWith("pi_");
    }
}
