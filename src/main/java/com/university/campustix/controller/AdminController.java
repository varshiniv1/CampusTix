package com.university.campustix.controller;

import com.university.campustix.dto.AnalyticsResponse;
import com.university.campustix.model.Booking;
import com.university.campustix.model.Event;
import com.university.campustix.repository.BookingRepository;
import com.university.campustix.repository.EventRepository;
import com.university.campustix.repository.SeatRepository;
import com.university.campustix.repository.WaitlistRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only: event management and analytics")
public class AdminController {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final WaitlistRepository waitlistRepository;
    private final AuthenticationManager authenticationManager;
    private final HttpSessionSecurityContextRepository securityContextRepository;

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            return ResponseEntity.ok().build();
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/auth")
    public ResponseEntity<Void> checkAuth() {
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get all events")
    @GetMapping("/events/all")
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @Operation(summary = "Get a single event by ID (cached)")
    @Cacheable(value = "events", key = "#id")
    @GetMapping("/events/{id}")
    public Event getEventById(@PathVariable Long id) {
        return eventRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Event not found: " + id));
    }

    @Operation(summary = "Create an event and auto-generate seats (clears events cache)")
    @CacheEvict(value = "events", allEntries = true)
    @PostMapping("/events")
    public Event createEvent(@RequestBody Event event, @RequestParam int seatCount) {
        if (event.getExpiryDate() == null && event.getEventTime() != null) {
            event.setExpiryDate(event.getEventTime().plusHours(24));
        }
        Event saved = eventRepository.save(event);

        List<com.university.campustix.model.Seat> seats = new ArrayList<>();
        for (int i = 1; i <= seatCount; i++) {
            com.university.campustix.model.Seat seat = new com.university.campustix.model.Seat();
            seat.setEvent(saved);
            seat.setSeatNumber("S" + i);
            seat.setStatus("AVAILABLE");
            seat.setVersion(0L);
            seats.add(seat);
        }
        seatRepository.saveAll(seats);
        return saved;
    }

    @Operation(summary = "Analytics dashboard — bookings, revenue, waitlist, recent activity")
    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        List<Event> events = eventRepository.findAll();
        List<Booking> allBookings = bookingRepository.findAll();

        List<Booking> confirmed = allBookings.stream()
            .filter(b -> !"CANCELLED".equals(b.getStatus()))
            .toList();

        long totalBookings = confirmed.size();
        double totalRevenue = confirmed.stream()
            .mapToDouble(b -> b.getEvent().getPrice() != null ? b.getEvent().getPrice() : 0.0)
            .sum();
        long totalWaitlisted = waitlistRepository.count();

        Map<Long, Long> bookingsByEvent = confirmed.stream()
            .collect(Collectors.groupingBy(b -> b.getEvent().getId(), Collectors.counting()));

        List<AnalyticsResponse.EventStat> topEvents = events.stream()
            .map(e -> {
                long bookings = bookingsByEvent.getOrDefault(e.getId(), 0L);
                long available = seatRepository.countByEventIdAndStatus(e.getId(), "AVAILABLE");
                long waitlisted = waitlistRepository.countByEventIdAndNotifiedFalse(e.getId());
                double revenue = bookings * (e.getPrice() != null ? e.getPrice() : 0.0);
                return new AnalyticsResponse.EventStat(
                    e.getName(), e.getCategory(), bookings, available, waitlisted, revenue
                );
            })
            .sorted((a, b) -> Long.compare(b.bookings(), a.bookings()))
            .limit(10)
            .toList();

        List<AnalyticsResponse.RecentBooking> recent = bookingRepository
            .findTop10ByOrderByBookedAtDesc()
            .stream()
            .map(b -> new AnalyticsResponse.RecentBooking(
                b.getBookingReference(),
                b.getBuyerName(),
                b.getBuyerEmail(),
                b.getEvent().getName(),
                b.getSeat().getSeatNumber(),
                b.getStatus(),
                b.getBookedAt() != null ? b.getBookedAt().toString() : ""
            ))
            .toList();

        return ResponseEntity.ok(new AnalyticsResponse(
            events.size(), totalBookings, totalRevenue, totalWaitlisted, topEvents, recent
        ));
    }
}
