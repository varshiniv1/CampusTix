package com.university.campustix.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info = @Info(
        title = "CampusTix API",
        version = "1.0",
        description = "High-concurrency university event ticketing platform. " +
            "Features: Redis rate limiting, WebSocket real-time seat updates, " +
            "@Async booking processing with optimistic locking, AI-powered descriptions, " +
            "QR code tickets, waitlist management, and Prometheus observability.",
        contact = @Contact(name = "CampusTix", email = "pcmbwow@gmail.com")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local Development")
    }
)
@Configuration
public class OpenApiConfig {}
