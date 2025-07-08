package com.minelsaygisever.fxtrackr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A dedicated service to interact with the Redis cache for exchange rates.
 */
@Slf4j
@Service
public class ExchangeRateCacheService {

    private static final String RATES_CACHE_KEY = "exchange_rates:latest";

    @Value("${caching.redis.ttl-minutes}")
    private long cacheTtlMinutes;

    private final RedisTemplate<String, Object> redisTemplate;

    public ExchangeRateCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Retrieves the entire map of exchange rates from the Redis cache.
     * @return An Optional containing the map, or empty if cache is unavailable or empty.
     */
    public Optional<Map<Object, Object>> getRatesMap() {
        try {
            Map<Object, Object> rates = redisTemplate.opsForHash().entries(RATES_CACHE_KEY);
            if (rates.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(rates);
        } catch (RedisConnectionFailureException e) {
            log.warn("Could not connect to Redis to get rates map.", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Writes a map of exchange rates to the Redis cache.
     * @param rates A map of currency codes to their rates.
     */
    public void updateRates(Map<String, BigDecimal> rates) {
        try {
            redisTemplate.opsForHash().putAll(RATES_CACHE_KEY, rates);
            redisTemplate.expire(RATES_CACHE_KEY, cacheTtlMinutes, TimeUnit.MINUTES);
            log.info("Successfully updated Redis cache. It will expire in {} minutes.", cacheTtlMinutes);
        } catch (RedisConnectionFailureException e) {
            log.warn("Could not connect to Redis to update the cache.", e.getMessage());
        }
    }
}