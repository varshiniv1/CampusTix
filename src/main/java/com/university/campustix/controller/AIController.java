package com.university.campustix.controller;

import com.university.campustix.dto.ChatRequest;
import com.university.campustix.dto.DescribeRequest;
import com.university.campustix.model.Event;
import com.university.campustix.repository.EventRepository;
import com.university.campustix.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final EventRepository eventRepository;

    @PostMapping("/describe")
    public ResponseEntity<String> describe(@RequestBody DescribeRequest req) {
        String description = aiService.generateEventDescription(
                req.getEventName(), req.getVenue(), req.getCategory(), req.getPrice()
        );
        return ResponseEntity.ok(description);
    }

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody ChatRequest req) {
        Event event = eventRepository.findById(req.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));
        String reply = aiService.chat(
                event.getName(),
                event.getVenue(),
                event.getEventTime() != null ? event.getEventTime().toString() : "TBD",
                event.getDescription(),
                req.getMessage()
        );
        return ResponseEntity.ok(reply);
    }
}
