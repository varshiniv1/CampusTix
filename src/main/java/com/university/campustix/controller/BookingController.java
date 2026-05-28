package com.university.campustix.controller;

import com.university.campustix.dto.BookingResponse;
import com.university.campustix.model.Booking;
import com.university.campustix.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingRepository bookingRepository;

    @GetMapping("/by-email")
    public ResponseEntity<List<BookingResponse>> getByEmail(@RequestParam String email) {
        List<BookingResponse> bookings = bookingRepository
                .findByBuyerEmailOrderByBookedAtDesc(email)
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(bookings);
    }

    private BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .bookingReference(b.getBookingReference())
                .eventName(b.getEvent().getName())
                .eventImageUrl(b.getEvent().getImageUrl())
                .venue(b.getEvent().getVenue())
                .eventTime(b.getEvent().getEventTime() != null ? b.getEvent().getEventTime().toString() : "TBD")
                .seatNumber(b.getSeat().getSeatNumber())
                .status(b.getStatus())
                .qrCodeBase64(b.getQrCodeBase64())
                .bookedAt(b.getBookedAt() != null ? b.getBookedAt().toString() : "")
                .buyerName(b.getBuyerName())
                .build();
    }
}
