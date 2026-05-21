package com.university.campustix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitingService {
    private final StringRedisTemplate redisTemplate;

    public boolean isAllowed(String studentId) {
        String key = "rate_limit:" + studentId;
        // Check if student has made a request in the last 5 seconds
        Boolean alreadyExists = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(alreadyExists)) {
            return false; // Request blocked
        }

        // Set key with 5-second expiration
        redisTemplate.opsForValue().set(key, "active", 5, TimeUnit.SECONDS);
        return true;
    }
}