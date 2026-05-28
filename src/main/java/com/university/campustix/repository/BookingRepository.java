package com.university.campustix.repository;

import com.university.campustix.model.Booking;
import com.university.campustix.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByBuyerEmailOrderByBookedAtDesc(String email);
    List<Booking> findByUserOrderByBookedAtDesc(User user);
    List<Booking> findTop10ByOrderByBookedAtDesc();

    Optional<Booking> findByBookingReference(String bookingReference);

    boolean existsByBuyerEmailAndEvent_IdAndStatusNot(String buyerEmail, Long eventId, String status);

    List<Booking> findByEvent_EventTimeBetweenAndReminded24hFalseAndStatusNot(
            LocalDateTime from, LocalDateTime to, String status);

    List<Booking> findByEvent_EventTimeBetweenAndReminded1hFalseAndStatusNot(
            LocalDateTime from, LocalDateTime to, String status);
}
