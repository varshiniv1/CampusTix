package com.university.campustix.service;

import com.university.campustix.model.Booking;
import com.university.campustix.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final BookingRepository bookingRepository;
    private final EmailService emailService;

    @Scheduled(fixedRate = 1_800_000) // every 30 minutes
    @Transactional
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();

        // 24-hour window: events happening between now+23h and now+25h
        List<Booking> remind24h = bookingRepository
            .findByEvent_EventTimeBetweenAndReminded24hFalseAndStatusNot(
                now.plusHours(23), now.plusHours(25), "CANCELLED");

        for (Booking b : remind24h) {
            try {
                emailService.sendReminder(
                    b.getBuyerEmail(), b.getBuyerName(),
                    b.getEvent().getName(), b.getEvent().getVenue(),
                    b.getEvent().getEventTime() != null ? b.getEvent().getEventTime().toString() : "TBD",
                    "24 hours", b.getSeat().getSeatNumber()
                );
                b.setReminded24h(true);
                bookingRepository.save(b);
            } catch (Exception e) {
                log.error("24h reminder failed for booking {}: {}", b.getBookingReference(), e.getMessage());
            }
        }

        // 1-hour window: events happening between now+45min and now+75min
        List<Booking> remind1h = bookingRepository
            .findByEvent_EventTimeBetweenAndReminded1hFalseAndStatusNot(
                now.plusMinutes(45), now.plusMinutes(75), "CANCELLED");

        for (Booking b : remind1h) {
            try {
                emailService.sendReminder(
                    b.getBuyerEmail(), b.getBuyerName(),
                    b.getEvent().getName(), b.getEvent().getVenue(),
                    b.getEvent().getEventTime() != null ? b.getEvent().getEventTime().toString() : "TBD",
                    "1 hour", b.getSeat().getSeatNumber()
                );
                b.setReminded1h(true);
                bookingRepository.save(b);
            } catch (Exception e) {
                log.error("1h reminder failed for booking {}: {}", b.getBookingReference(), e.getMessage());
            }
        }

        if (!remind24h.isEmpty() || !remind1h.isEmpty()) {
            log.info("Reminders sent: {} (24h), {} (1h)", remind24h.size(), remind1h.size());
        }
    }
}
