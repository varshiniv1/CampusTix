package com.university.campustix.controller;

import com.university.campustix.model.Seat;
import com.university.campustix.repository.SeatRepository;
import com.university.campustix.service.BookingService;
import com.university.campustix.service.RateLimitingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final RateLimitingService rateLimiter;
    private final BookingService bookingService;
    private final SeatRepository seatRepository;

    @GetMapping("/seats")
    public List<Seat> getSeatsByEvent(@RequestParam Long eventId) {
        return seatRepository.findByEventId(eventId);
    }

    @PostMapping("/claim")
    public ResponseEntity<String> claimTicket(
            @RequestParam String studentId,
            @RequestParam String seatId,
            @RequestParam String studentName,
            @RequestParam(required = false) String paymentIntentId) {

        if (!rateLimiter.isAllowed(studentId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit hit.");
        }
        bookingService.processBooking(studentId, Long.parseLong(seatId), studentName, paymentIntentId);
        return ResponseEntity.accepted().body("Request queued!");
    }
}
