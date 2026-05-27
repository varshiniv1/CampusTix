package com.university.campustix.service;

import com.university.campustix.model.Booking;
import com.university.campustix.model.Event;
import com.university.campustix.model.Seat;
import com.university.campustix.repository.BookingRepository;
import com.university.campustix.repository.EventRepository;
import com.university.campustix.repository.SeatRepository;
import com.university.campustix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final QRCodeService qrCodeService;
    private final SimpMessagingTemplate messagingTemplate;

    private String toChannelSuffix(String email) {
        return java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(email.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @KafkaListener(topics = "campustix-topic", groupId = "campustix-group")
    @Transactional
    public void consumeBookingRequest(String message) {
        // Message format: "email:seatId:name"
        String[] parts = message.split(":");
        String email = parts[0];
        Long seatId = Long.parseLong(parts[1]);
        String name = parts[2];

        try {
            Seat seat = seatRepository.findById(seatId).orElseThrow();
            Event event = eventRepository.findById(seat.getEvent().getId()).orElseThrow();

            seat.setStatus("SOLD");
            seatRepository.save(seat);

            String ref = "CT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            String qrContent = String.format(
                "CAMPUSTIX TICKET\nRef: %s\nEvent: %s\nSeat: %s\nAttendee: %s\nVenue: %s",
                ref, event.getName(), seat.getSeatNumber(), name, event.getVenue()
            );
            String qrCode = qrCodeService.generateQRCode(qrContent);

            Booking booking = Booking.builder()
                    .buyerEmail(email)
                    .buyerName(name)
                    .seat(seat)
                    .event(event)
                    .bookingReference(ref)
                    .bookedAt(LocalDateTime.now())
                    .status("CONFIRMED")
                    .qrCodeBase64(qrCode)
                    .build();

            userRepository.findByEmail(email).ifPresent(booking::setUser);
            bookingRepository.save(booking);

            emailService.sendBookingConfirmation(
                    email, name, event.getName(),
                    seat.getSeatNumber(), event.getVenue(),
                    event.getEventTime() != null ? event.getEventTime().toString() : "TBD",
                    qrCode
            );

            // Channel is derived from email so the specific client can subscribe to it
            String channel = "/topic/status/" + toChannelSuffix(email);
            messagingTemplate.convertAndSend(channel,
                    "SUCCESS: Booking confirmed! Ref: " + ref + " | Seat: " + seat.getSeatNumber());
            messagingTemplate.convertAndSend("/topic/seats-update", "refresh");

        } catch (Exception e) {
            e.printStackTrace();
            messagingTemplate.convertAndSend("/topic/status/" + toChannelSuffix(email),
                    "ERROR: Booking failed. The seat may have already been taken.");
        }
    }
}
