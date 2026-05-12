package com.jutjubic.jutjubic_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;

    public RateLimiterService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Provera i inkrement rate limita
     * @param userId ID korisnika
     * @param limit Maksimalan broj zahteva
     * @param windowSeconds Vremenski prozor u sekundama
     * @return true ako je dozvoljen zahtev, false ako je dostignut limit
     */
    public boolean checkAndIncrement(Long userId, int limit, long windowSeconds) {
        String key = "rate_limit:comment:" + userId;

        try {
            // Dobavi trenutnu vrednost
            String currentValueStr = redisTemplate.opsForValue().get(key);
            long currentValue = currentValueStr != null ? Long.parseLong(currentValueStr) : 0;

            log.debug("Rate limit check for user {}: {}/{}", userId, currentValue, limit);

            if (currentValue >= limit) {
                log.warn("Rate limit EXCEEDED for user {}: {}/{}", userId, currentValue, limit);
                return false;  // Limit dostignut
            }

            // Inkrementiraj
            Long newValue = redisTemplate.opsForValue().increment(key);

            // Postavi expiration samo ako je ključ nov (prvi put se postavlja)
            if (newValue != null && newValue == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
                log.debug("Set expiration for key {} to {} seconds", key, windowSeconds);
            }

            log.info("Rate limit OK for user {}: {}/{}", userId, newValue, limit);
            return true;  // Dozvoljen zahtev

        } catch (Exception e) {
            log.error("Redis rate limiter error for user {}", userId, e);
            // Ako Redis padne, dozvoli zahtev (fail-open)
            return true;
        }
    }

    /**
     * Dobavi trenutni broj zahteva za korisnika
     */
    public long getCurrentCount(Long userId) {
        String key = "rate_limit:comment:" + userId;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }

    /**
     * Resetuj rate limit za korisnika (za testiranje)
     */
    public void reset(Long userId) {
        String key = "rate_limit:comment:" + userId;
        redisTemplate.delete(key);
        log.info("Reset rate limit for user {}", userId);
    }
}