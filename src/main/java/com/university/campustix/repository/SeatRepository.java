package com.university.campustix.repository;

import com.university.campustix.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    // Corrected to look inside the Event object for the ID
    List<Seat> findByEventId(Long eventId);
    long countByEventIdAndStatus(Long eventId, String status);
}