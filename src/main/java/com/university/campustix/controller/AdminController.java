package com.university.campustix.controller;

import com.university.campustix.model.Event;
import com.university.campustix.model.Seat;
import com.university.campustix.repository.EventRepository;
import com.university.campustix.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    @GetMapping("/auth")
    public ResponseEntity<Void> checkAuth() {
        return ResponseEntity.ok().build();
    }

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    /**
     * Retrieves all events for the marketplace.
     * This matches the fetch('/api/v1/admin/events/all') call in your frontend.
     */
    @GetMapping("/events/all")
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    /**
     * Retrieves a single event. Used by booking.html to load
     * specific posters, map links, and expiry status.
     */
    @GetMapping("/events/{id}")
    public Event getEventById(@PathVariable Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
    }

    /**
     * Creates an event and its associated stadium seats.
     * Includes logic to handle the expiryDate field.
     */
    @PostMapping("/events")
    public Event createEvent(@RequestBody Event event, @RequestParam int seatCount) {
        // Logic: Handle Event Expiry
        // If no expiry is provided, default it to 24 hours after the event starts.
        if (event.getExpiryDate() == null && event.getEventTime() != null) {
            event.setExpiryDate(event.getEventTime().plusHours(24));
        }

        // 1. Persist the Event metadata
        Event savedEvent = eventRepository.save(event);

        // 2. Automatically generate the stadium seats
        List<Seat> seats = new ArrayList<>();
        for (int i = 1; i <= seatCount; i++) {
            Seat seat = new Seat();
            // Link the seat to the event object (JPA Relationship)
            seat.setEvent(savedEvent);
            seat.setSeatNumber("S" + i);
            seat.setStatus("AVAILABLE");
            seat.setVersion(0L); // For Optimistic Locking
            seats.add(seat);
        }

        // 3. Save all seats in a single batch for performance
        seatRepository.saveAll(seats);

        return savedEvent;
    }
}