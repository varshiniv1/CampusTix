package com.university.campustix.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps the primary CacheManager so every cache.get() is counted as a hit or miss.
 * Exposes cache.gets{result, cache} to Prometheus via Micrometer.
 */
public class MetricsCacheManager implements CacheManager {

    private final CacheManager delegate;
    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, MetricsCache> wrappedCaches = new ConcurrentHashMap<>();

    public MetricsCacheManager(CacheManager delegate, MeterRegistry registry) {
        this.delegate = delegate;
        this.registry = registry;
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = delegate.getCache(name);
        if (cache == null) return null;
        return wrappedCaches.computeIfAbsent(name, k -> new MetricsCache(cache, registry));
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }
}
