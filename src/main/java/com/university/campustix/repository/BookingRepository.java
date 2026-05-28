package com.university.campustix.repository;

import com.university.campustix.model.Booking;
import com.university.campustix.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByBuyerEmailOrderByBookedAtDesc(String email);
    List<Booking> findByUserOrderByBookedAtDesc(User user);
    List<Booking> findTop10ByOrderByBookedAtDesc();
}
