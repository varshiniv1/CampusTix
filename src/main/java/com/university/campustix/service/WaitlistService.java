package com.university.campustix.service;

import com.university.campustix.model.Waitlist;
import com.university.campustix.repository.EventRepository;
import com.university.campustix.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final EventRepository eventRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Map<String, Object> joinWaitlist(Long eventId, String email, String name) {
        if (waitlistRepository.findByEventIdAndBuyerEmail(eventId, email).isPresent()) {
            long pos = getPosition(eventId, email);
            return Map.of("status", "already_registered", "position", pos);
        }
        Waitlist entry = Waitlist.builder()
            .eventId(eventId)
            .buyerEmail(email)
            .buyerName(name)
            .build();
        waitlistRepository.save(entry);

        long position = waitlistRepository.countByEventIdAndNotifiedFalse(eventId);
        return Map.of("status", "joined", "position", position);
    }

    public long getPosition(Long eventId, String email) {
        return waitlistRepository.findByEventIdAndBuyerEmail(eventId, email)
            .map(w -> waitlistRepository
                .countByEventIdAndNotifiedFalseAndAddedAtBefore(eventId, w.getAddedAt()) + 1)
            .orElse(-1L);
    }

    @Async
    @Transactional
    public void notifyNext(Long eventId) {
        List<Waitlist> queue = waitlistRepository
            .findByEventIdAndNotifiedFalseOrderByAddedAtAsc(eventId);
        if (queue.isEmpty()) return;

        Waitlist next = queue.get(0);
        next.setNotified(true);
        waitlistRepository.save(next);

        String eventName = eventRepository.findById(eventId)
            .map(e -> e.getName())
            .orElse("an event");

        emailService.sendWaitlistNotification(next.getBuyerEmail(), next.getBuyerName(), eventName);
        messagingTemplate.convertAndSend("/topic/seats-update", "refresh");
    }
}
