package com.university.campustix.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private String buyerEmail;

    private String buyerName;

    private LocalDateTime addedAt;

    @Builder.Default
    private boolean notified = false;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }
}
