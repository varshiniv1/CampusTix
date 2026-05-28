package com.university.campustix.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.ui.Model;

@Controller
public class ViewController {

    @GetMapping("/")
    public String homePage() { return "index"; }

    @GetMapping("/events")
    public String eventsPage() { return "events"; }

    @GetMapping("/booking/{eventId}")
    public String bookingPage(@PathVariable Long eventId, Model model) {
        model.addAttribute("eventId", eventId);
        return "booking";
    }

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @GetMapping("/my-tickets")
    public String myTicketsPage() { return "my-tickets"; }

    @GetMapping("/admin")
    public String adminPage() { return "admin"; }

    @GetMapping("/admin/analytics")
    public String analyticsPage() { return "admin-analytics"; }
}
