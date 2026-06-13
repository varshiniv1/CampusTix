package com.university.campustix.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class BookingMetricsService {

    private final MeterRegistry registry;
    private final AtomicLong activeBookings = new AtomicLong(0);

    @PostConstruct
    public void init() {
        Gauge.builder("booking.concurrent.active", activeBookings, AtomicLong::get)
            .description("Booking requests currently in-flight")
            .register(registry);
    }

    public void recordStart() {
        activeBookings.incrementAndGet();
    }

    // status: "success" | "conflict" | "error"
    public void recordEnd(String status, long durationNanos) {
        activeBookings.decrementAndGet();
        Counter.builder("booking.requests.total")
            .tag("status", status)
            .register(registry)
            .increment();
        Timer.builder("booking.processing.seconds")
            .tag("status", status)
            .publishPercentiles(0.50, 0.95, 0.99)
            .publishPercentileHistogram()
            .register(registry)
            .record(durationNanos, TimeUnit.NANOSECONDS);
    }
}
