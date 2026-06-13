package com.university.campustix;

import com.university.campustix.model.Event;
import com.university.campustix.model.Seat;
import com.university.campustix.repository.BookingRepository;
import com.university.campustix.repository.EventRepository;
import com.university.campustix.repository.SeatRepository;
import com.university.campustix.service.BookingService;
import com.university.campustix.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates exactly-once seat assignment under 500 simultaneous booking requests.
 *
 * Uses SyncTaskExecutor so @Async runs synchronously in each calling thread,
 * giving us true multi-thread concurrency with deterministic completion.
 * Optimistic locking (@Version on Seat) is exercised against a real PostgreSQL
 * container — not mocked.
 *
 * Resume claim: "guaranteed exactly-once seat assignment under 500 concurrent
 * requests via optimistic locking, validated in integration tests"
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Import(ConcurrencyLoadTest.SyncAsyncConfig.class)
class ConcurrencyLoadTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis =
        new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    // Make @Async run synchronously so each test thread blocks until its booking completes
    @TestConfiguration
    static class SyncAsyncConfig implements AsyncConfigurer {
        @Override
        public Executor getAsyncExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @MockitoBean EmailService emailService;
    @MockitoBean SimpMessagingTemplate messagingTemplate;

    @Autowired BookingService bookingService;
    @Autowired BookingRepository bookingRepository;
    @Autowired SeatRepository seatRepository;
    @Autowired EventRepository eventRepository;

    private Seat testSeat;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        seatRepository.deleteAll();
        eventRepository.deleteAll();

        Event event = new Event();
        event.setName("Perf Test Concert");
        event.setVenue("Test Arena");
        event = eventRepository.save(event);

        testSeat = new Seat();
        testSeat.setEvent(event);
        testSeat.setSeatNumber("A1");
        testSeat.setStatus("AVAILABLE");
        testSeat = seatRepository.save(testSeat);
    }

    @Test
    void fiveHundredConcurrentRequests_exactlyOneBookingConfirmed() throws Exception {
        int N = 500;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate  = new CountDownLatch(N);
        ExecutorService pool = Executors.newFixedThreadPool(N);

        long startNanos = System.nanoTime();

        for (int i = 0; i < N; i++) {
            final String email = "perf" + i + "@test.com";
            pool.submit(() -> {
                try {
                    startGate.await(); // all threads wait here
                    bookingService.processBooking(email, testSeat.getId(), "Perf User");
                } catch (Exception ignored) {
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown(); // release all 500 simultaneously
        boolean completed = doneGate.await(60, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(completed).as("All 500 booking attempts must finish within 60s").isTrue();

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        long confirmed = bookingRepository.findAll().stream()
            .filter(b -> "CONFIRMED".equals(b.getStatus())).count();

        System.out.printf(
            "%n[CONCURRENCY] %d concurrent requests | 1 confirmed | %d conflicts/errors | %dms total%n",
            N, N - confirmed, elapsedMs
        );

        assertThat(confirmed).as("Exactly ONE booking should be confirmed").isEqualTo(1L);

        Seat updated = seatRepository.findById(testSeat.getId()).orElseThrow();
        assertThat(updated.getStatus()).as("Seat must be SOLD").isEqualTo("SOLD");
    }
}
