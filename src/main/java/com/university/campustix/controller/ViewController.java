package com.university.campustix.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.ui.Model;

@Controller
public class ViewController {

    @GetMapping("/")
    public String eventsPage() {
        return "events"; // Landing page with list of events
    }

    @GetMapping("/booking/{eventId}")
    public String bookingPage(@PathVariable Long eventId, Model model) {
        // We pass the eventId to the HTML so the JavaScript can use it
        model.addAttribute("eventId", eventId);
        return "booking";
    }

    @GetMapping("/admin")
    public String adminPage() {
        return "admin";
    }
}