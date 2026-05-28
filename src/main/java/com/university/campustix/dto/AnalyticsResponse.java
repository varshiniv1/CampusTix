package com.university.campustix.dto;

import java.util.List;

public record AnalyticsResponse(
    long totalEvents,
    long totalBookings,
    double totalRevenue,
    long totalWaitlisted,
    List<EventStat> topEvents,
    List<RecentBooking> recentBookings
) {
    public record EventStat(
        String eventName,
        String category,
        long bookings,
        long availableSeats,
        long waitlisted,
        double revenue
    ) {}

    public record RecentBooking(
        String ref,
        String buyerName,
        String buyerEmail,
        String eventName,
        String seatNumber,
        String status,
        String bookedAt
    ) {}
}
