package com.university.campustix.repository;

import com.university.campustix.model.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    List<Waitlist> findByEventIdAndNotifiedFalseOrderByAddedAtAsc(Long eventId);

    Optional<Waitlist> findByEventIdAndBuyerEmail(Long eventId, String buyerEmail);

    long countByEventIdAndNotifiedFalse(Long eventId);

    long countByEventIdAndNotifiedFalseAndAddedAtBefore(Long eventId, LocalDateTime addedAt);
}
