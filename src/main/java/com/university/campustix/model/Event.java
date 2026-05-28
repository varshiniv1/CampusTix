package com.university.campustix.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String venue;
    private String venueAddress;
    private LocalDateTime eventTime;
    private LocalDateTime expiryDate; // NEW: The end date for the event
    private Double price;
    private String imageUrl;
    private String contactInfo;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String category; // MUSIC, SPORTS, COMEDY, ARTS, MOVIES, FAMILY, OTHER
}