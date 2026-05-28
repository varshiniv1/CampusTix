package com.university.campustix.service;

import com.university.campustix.model.Booking;
import com.university.campustix.model.Event;
import com.university.campustix.model.Seat;
import com.university.campustix.repository.BookingRepository;
import com.university.campustix.repository.EventRepository;
import com.university.campustix.repository.SeatRepository;
import com.university.campustix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final QRCodeService qrCodeService;
    private final WaitlistService waitlistService;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @Transactional
    public void processBooking(String email, Long seatId, String name) {
        String channel = "/topic/status/" + toChannelSuffix(email);
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

            messagingTemplate.convertAndSend(channel,
                "SUCCESS: Booking confirmed! Ref: " + ref + " | Seat: " + seat.getSeatNumber());
            messagingTemplate.convertAndSend("/topic/seats-update", "refresh");

        } catch (Exception e) {
            messagingTemplate.convertAndSend(channel,
                "ERROR: Booking failed. The seat may have already been taken.");
        }
    }

    @Async
    @Transactional
    public void cancelBooking(Long bookingId, String email) {
        String channel = "/topic/status/" + toChannelSuffix(email);
        try {
            Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

            if (!booking.getBuyerEmail().equalsIgnoreCase(email)) {
                messagingTemplate.convertAndSend(channel, "ERROR: Unauthorized cancellation attempt.");
                return;
            }

            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);

            Seat seat = booking.getSeat();
            seat.setStatus("AVAILABLE");
            seatRepository.save(seat);

            // Notify next person on waitlist for this event
            waitlistService.notifyNext(booking.getEvent().getId());

            messagingTemplate.convertAndSend(channel, "CANCELLED: Your booking has been cancelled.");
            messagingTemplate.convertAndSend("/topic/seats-update", "refresh");

        } catch (Exception e) {
            messagingTemplate.convertAndSend(channel, "ERROR: Cancellation failed — " + e.getMessage());
        }
    }

    private String toChannelSuffix(String email) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(email.getBytes(StandardCharsets.UTF_8));
    }
}
