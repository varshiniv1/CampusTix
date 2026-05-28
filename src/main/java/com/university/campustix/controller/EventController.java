package com.university.campustix.controller;

import com.university.campustix.model.Event;
import com.university.campustix.repository.EventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Public event search and discovery")
public class EventController {

    private final EventRepository eventRepository;

    @Operation(summary = "Search and filter events — all params optional")
    @GetMapping("/search")
    public List<Event> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false, defaultValue = "date") String sort) {

        Stream<Event> stream = eventRepository.findAll().stream();

        if (q != null && !q.isBlank()) {
            String lower = q.toLowerCase();
            stream = stream.filter(e ->
                contains(e.getName(), lower) ||
                contains(e.getCategory(), lower) ||
                contains(e.getDescription(), lower) ||
                contains(e.getVenue(), lower)
            );
        }

        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            stream = stream.filter(e -> category.equalsIgnoreCase(e.getCategory()));
        }

        if (minPrice != null) stream = stream.filter(e -> e.getPrice() != null && e.getPrice() >= minPrice);
        if (maxPrice != null) stream = stream.filter(e -> e.getPrice() != null && e.getPrice() <= maxPrice);

        Comparator<Event> comparator = switch (sort) {
            case "price_asc"  -> Comparator.comparingDouble(e -> (e.getPrice() != null ? e.getPrice() : 0));
            case "price_desc" -> Comparator.comparingDouble((Event e) -> (e.getPrice() != null ? e.getPrice() : 0)).reversed();
            default           -> Comparator.comparing(e -> (e.getEventTime() != null ? e.getEventTime() : LocalDateTime.MAX));
        };

        return stream.sorted(comparator).toList();
    }

    private boolean contains(String field, String query) {
        return field != null && field.toLowerCase().contains(query);
    }
}
