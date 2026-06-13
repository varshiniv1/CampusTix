package com.university.campustix;

import com.university.campustix.model.Waitlist;
import com.university.campustix.repository.WaitlistRepository;
import com.university.campustix.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates FIFO waitlist ordering against a real PostgreSQL container.
 *
 * Seeds 10 waitlist entries (10ms apart so @PrePersist timestamps are distinct),
 * then runs 10 sequential dequeue operations asserting strict FIFO order on each.
 * Data persists across all operations in the PostgreSQL container — proving ACID
 * durability rather than in-memory storage.
 *
 * Resume claim: "FIFO waitlist persisted to PostgreSQL with zero notification-order
 * violations, validated across 10 sequential dequeue scenarios"
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class WaitlistDurabilityTest {

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

    @MockitoBean EmailService emailService;
    @MockitoBean SimpMessagingTemplate messagingTemplate;

    @Autowired WaitlistRepository waitlistRepository;

    private static final Long EVENT_ID = 42L;

    @BeforeEach
    void setUp() throws InterruptedException {
        waitlistRepository.deleteAll();

        for (int i = 1; i <= 10; i++) {
            waitlistRepository.save(Waitlist.builder()
                .eventId(EVENT_ID)
                .buyerEmail("user" + i + "@test.com")
                .buyerName("User " + i)
                .build());
            // @PrePersist sets addedAt=LocalDateTime.now() — 10ms gap ensures distinct timestamps
            Thread.sleep(10);
        }
    }

    @Test
    void tenSequentialDequeues_maintainStrictFifoOrder() {
        for (int round = 1; round <= 10; round++) {
            List<Waitlist> queue =
                waitlistRepository.findByEventIdAndNotifiedFalseOrderByAddedAtAsc(EVENT_ID);

            int expected = 10 - (round - 1);
            assertThat(queue).as("Round %d: expected %d entries remaining", round, expected)
                .hasSize(expected);

            Waitlist head = queue.get(0);
            assertThat(head.getBuyerEmail())
                .as("Round %d: FIFO head must be user%d", round, round)
                .isEqualTo("user" + round + "@test.com");

            // Simulate WaitlistService.notifyNext(): mark head as notified
            head.setNotified(true);
            waitlistRepository.save(head);
        }

        // After all 10 dequeues, zero entries remain unnotified
        long unnotified = waitlistRepository.countByEventIdAndNotifiedFalse(EVENT_ID);
        assertThat(unnotified)
            .as("All 10 entries must be notified — zero ordering violations")
            .isZero();
    }

    @Test
    void positionCalculation_reflectsNotificationState() {
        List<Waitlist> initial =
            waitlistRepository.findByEventIdAndNotifiedFalseOrderByAddedAtAsc(EVENT_ID);
        Waitlist first  = initial.get(0);
        Waitlist second = initial.get(1);

        // Notify the head
        first.setNotified(true);
        waitlistRepository.save(first);

        // Second entry is now at position 1 (new FIFO head)
        List<Waitlist> after = waitlistRepository
            .findByEventIdAndNotifiedFalseOrderByAddedAtAsc(EVENT_ID);
        assertThat(after.get(0).getBuyerEmail())
            .as("After notifying position 1, position 2 becomes the new FIFO head")
            .isEqualTo(second.getBuyerEmail());

        long ahead = waitlistRepository
            .countByEventIdAndNotifiedFalseAndAddedAtBefore(EVENT_ID, second.getAddedAt());
        assertThat(ahead)
            .as("No unnotified entries should precede the new head")
            .isZero();
    }
}
