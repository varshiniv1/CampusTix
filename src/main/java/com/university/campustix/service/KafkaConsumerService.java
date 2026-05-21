package com.university.campustix.service;

import com.university.campustix.model.Event;
import com.university.campustix.model.Seat;
import com.university.campustix.repository.EventRepository;
import com.university.campustix.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final EmailService emailService;

    @KafkaListener(topics = "campustix-topic", groupId = "campustix-group")
    @Transactional
    public void consumeBookingRequest(String message) {
        // Message Format: "user@umass.edu:101:Varshini"
        String[] parts = message.split(":");
        String recipientEmail = parts[0]; // DYNAMIC EMAIL
        Long seatId = Long.parseLong(parts[1]);
        String recipientName = parts[2]; // DYNAMIC NAME

        try {
            Seat seat = seatRepository.findById(seatId).orElseThrow();
            Event event = eventRepository.findById(seat.getEvent().getId()).orElseThrow();

            // Update DB
            seat.setStatus("SOLD");
            seatRepository.save(seat);

            // Trigger the email dynamically
            emailService.sendBookingConfirmation(
                    recipientEmail,
                    recipientName,
                    event.getName(),
                    seat.getSeatNumber(),
                    event.getVenue(),
                    event.getEventTime().toString()
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}