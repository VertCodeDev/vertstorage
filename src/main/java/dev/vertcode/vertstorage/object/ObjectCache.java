package dev.vertcode.vertstorage.object;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class is used to cache key-value pairs for a certain amount of time or
 * forever. This is useful for caching data that is used often and is expensive
 */
public class ObjectCache<K, V> {

    private final Map<K, V> cachedValues = new HashMap<>();
    private final Map<K, Instant> expirationTimes = new HashMap<>();

    private final long cacheTime;
    private final TimeUnit cacheTimeUnit;
    private final boolean cacheExpires;

    public ObjectCache(long cacheTime, TimeUnit cacheTimeUnit) {
        this.cacheTime = cacheTime;
        this.cacheTimeUnit = cacheTimeUnit;
        this.cacheExpires = true;
    }

    public ObjectCache() {
        this.cacheTime = 0;
        this.cacheTimeUnit = TimeUnit.SECONDS;
        this.cacheExpires = false;
    }

    /**
     * Gets the value for the given key.
     *
     * @param key The key to get the value for
     * @return The value for the given key
     */
    public @Nullable V get(K key) {
        if (!this.cacheExpires) {
            return this.cachedValues.get(key);
        }

        // First we check if the key is cached
        if (!isCached(key)) {
            return null;
        }

        // Now we know its cached, we can return the value since the isCached method also checks if the cache has expired
        return this.cachedValues.get(key);
    }

    /**
     * Puts the given key-value pair in the cache.
     *
     * @param key   The key
     * @param value The value
     */
    public void put(K key, V value) {
        if (!this.cacheExpires) {
            this.cachedValues.put(key, value);
            return;
        }

        // First we check if the key is cached
        if (isCached(key)) {
            // If it is, we remove it from the cache
            remove(key);
        }

        Instant expirationTime = Instant.now().plusMillis(this.cacheTimeUnit.toMillis(this.cacheTime));

        // Now we know its not cached, we can put it in the cache
        this.cachedValues.put(key, value);
        this.expirationTimes.put(key, expirationTime);
    }

    /**
     * Removes the given key from the cache.
     *
     * @param key The key to remove
     */
    public void remove(K key) {
        this.cachedValues.remove(key);
        this.expirationTimes.remove(key);
    }

    /**
     * Returns whether the given key is cached.
     *
     * @param key The key to check
     * @return Whether the given key is cached
     */
    public boolean isCached(K key) {
        if (!this.cacheExpires) {
            return this.cachedValues.containsKey(key);
        }

        // First we check if the key is cached
        if (!this.cachedValues.containsKey(key)) {
            return false;
        }

        // Get the expiration time
        Instant expirationTime = this.expirationTimes.get(key);
        // Check if the expiration time is before the current time
        boolean isExpired = expirationTime.isBefore(Instant.now());
        // If it is, we remove it from the cache
        if (isExpired) {
            remove(key);
        }

        // Return whether the key is cached
        return !isExpired;
    }

    /**
     * Clears the cache.
     */
    public void clear() {
        this.cachedValues.clear();
        this.expirationTimes.clear();
    }

    /**
     * Gets the cached values.
     *
     * @return The cached values
     */
    public List<V> getValues() {
        List<V> values = new ArrayList<>();

        for (K key : new ArrayList<>(this.cachedValues.keySet())) {
            if (!isCached(key)) {
                continue;
            }

            values.add(this.cachedValues.get(key));
        }

        return values;
    }
}
