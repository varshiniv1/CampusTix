package com.university.campustix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "campustix-topic";

    public void sendBookingRequest(String message) {
        // Asynchronously send to Kafka
        kafkaTemplate.send(TOPIC, message);
    }
}