package com.university.campustix.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;

/**
 * Wraps a Spring Cache to count hits and misses via Micrometer.
 * Exposes cache.gets{result="hit"|"miss", cache=name} counters.
 */
public class MetricsCache implements Cache {

    private final Cache delegate;
    private final Counter hitCounter;
    private final Counter missCounter;

    public MetricsCache(Cache delegate, MeterRegistry registry) {
        this.delegate = delegate;
        this.hitCounter = Counter.builder("cache.gets")
            .tag("result", "hit")
            .tag("cache", delegate.getName())
            .description("Cache hits")
            .register(registry);
        this.missCounter = Counter.builder("cache.gets")
            .tag("result", "miss")
            .tag("cache", delegate.getName())
            .description("Cache misses")
            .register(registry);
    }

    @Override
    public String getName() { return delegate.getName(); }

    @Override
    public Object getNativeCache() { return delegate.getNativeCache(); }

    @Override
    @Nullable
    public ValueWrapper get(Object key) {
        ValueWrapper value = delegate.get(key);
        if (value != null) hitCounter.increment();
        else missCounter.increment();
        return value;
    }

    @Override
    @Nullable
    public <T> T get(Object key, @Nullable Class<T> type) {
        T value = delegate.get(key, type);
        if (value != null) hitCounter.increment();
        else missCounter.increment();
        return value;
    }

    @Override
    @Nullable
    public <T> T get(Object key, Callable<T> valueLoader) {
        boolean hit = delegate.get(key) != null;
        if (hit) hitCounter.increment();
        else missCounter.increment();
        return delegate.get(key, valueLoader);
    }

    @Override
    public void put(Object key, @Nullable Object value) { delegate.put(key, value); }

    @Override
    public void evict(Object key) { delegate.evict(key); }

    @Override
    public void clear() { delegate.clear(); }
}
