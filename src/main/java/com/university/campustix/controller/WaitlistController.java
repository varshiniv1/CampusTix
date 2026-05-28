package com.university.campustix.controller;

import com.university.campustix.service.WaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/waitlist")
@RequiredArgsConstructor
@Tag(name = "Waitlist", description = "Join the waitlist for sold-out events")
public class WaitlistController {

    private final WaitlistService waitlistService;

    @Operation(summary = "Join the waitlist for a sold-out event")
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> join(
            @RequestParam Long eventId,
            @RequestParam String email,
            @RequestParam String name) {
        return ResponseEntity.ok(waitlistService.joinWaitlist(eventId, email, name));
    }

    @Operation(summary = "Get your position in the waitlist")
    @GetMapping("/position")
    public ResponseEntity<Map<String, Long>> position(
            @RequestParam Long eventId,
            @RequestParam String email) {
        long pos = waitlistService.getPosition(eventId, email);
        return ResponseEntity.ok(Map.of("position", pos));
    }
}
