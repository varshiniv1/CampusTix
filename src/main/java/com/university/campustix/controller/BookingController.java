package com.university.campustix.controller;

import com.university.campustix.dto.BookingResponse;
import com.university.campustix.model.Booking;
import com.university.campustix.repository.BookingRepository;
import com.university.campustix.service.BookingService;
import com.university.campustix.service.PdfTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "View and manage ticket bookings")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final PdfTicketService pdfTicketService;

    @Operation(summary = "Get all bookings for a given email address")
    @GetMapping("/by-email")
    public ResponseEntity<List<BookingResponse>> getByEmail(@RequestParam String email) {
        List<BookingResponse> bookings = bookingRepository
            .findByBuyerEmailOrderByBookedAtDesc(email)
            .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(bookings);
    }

    @Operation(summary = "Cancel a booking — frees the seat and notifies the waitlist")
    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<String> cancelBooking(
            @PathVariable Long id,
            @RequestParam String email) {
        bookingService.cancelBooking(id, email);
        return ResponseEntity.accepted().body("Cancellation processing.");
    }

    @Operation(summary = "Download a PDF ticket by booking reference")
    @GetMapping("/{ref}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String ref) throws IOException {
        Booking booking = bookingRepository.findByBookingReference(ref)
            .orElseThrow(() -> new RuntimeException("Booking not found: " + ref));
        byte[] pdf = pdfTicketService.generate(booking);
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"ticket-" + ref + ".pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
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
