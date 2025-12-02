package io.github.ilumar589.jecs.query;

import io.github.ilumar589.jecs.world.Archetype;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe cache for query results with LRU eviction.
 * Maps query signatures (cache keys) to lists of matching archetypes.
 * 
 * <h2>Cache Invalidation</h2>
 * The cache is invalidated (cleared) when new archetypes are created,
 * as new archetypes may match existing queries.
 * 
 * <h2>Thread Safety</h2>
 * This cache uses synchronized access to a LinkedHashMap for thread-safe 
 * operations with LRU eviction support.
 * 
 * <h2>Performance Benefits</h2>
 * <ul>
 *   <li>Eliminates repeated archetype filtering for identical queries</li>
 *   <li>O(1) cache lookup vs O(archetypes × types) filtering</li>
 *   <li>Particularly beneficial for game loops with repeated queries</li>
 * </ul>
 * 
 * <h2>Memory Management</h2>
 * <ul>
 *   <li>Maximum cache size is configurable (default: 256 entries)</li>
 *   <li>LRU eviction automatically removes least recently used entries</li>
 *   <li>Prevents unbounded memory growth in games with dynamic query patterns</li>
 * </ul>
 */
public final class QueryCache {
    
    /**
     * Default maximum cache size.
     * This is a reasonable default for most ECS applications.
     */
    public static final int DEFAULT_MAX_SIZE = 256;
    
    private final Map<QueryCacheKey, List<Archetype>> cache;
    private final int maxSize;
    
    /**
     * Creates a new empty query cache with default maximum size.
     */
    public QueryCache() {
        this(DEFAULT_MAX_SIZE);
    }
    
    /**
     * Creates a new empty query cache with the specified maximum size.
     *
     * @param maxSize the maximum number of cached entries (must be positive)
     */
    public QueryCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive: " + maxSize);
        }
        this.maxSize = maxSize;
        // LinkedHashMap with access order for LRU eviction
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<QueryCacheKey, List<Archetype>> eldest) {
                return size() > maxSize;
            }
        };
    }
    
    /**
     * Gets cached matching archetypes for a query, or null if not cached.
     * Accessing an entry updates its position in the LRU order.
     *
     * @param key the query cache key
     * @return the cached list of matching archetypes, or null if not in cache
     */
    public List<Archetype> get(QueryCacheKey key) {
        synchronized (cache) {
            return cache.get(key);
        }
    }
    
    /**
     * Puts matching archetypes into the cache for a query.
     * If the cache exceeds its maximum size, the least recently used entry is evicted.
     *
     * @param key the query cache key
     * @param archetypes the list of matching archetypes
     */
    public void put(QueryCacheKey key, List<Archetype> archetypes) {
        synchronized (cache) {
            cache.put(key, archetypes);
        }
    }
    
    /**
     * Invalidates (clears) the entire cache.
     * This should be called when new archetypes are created,
     * as they may match existing query patterns.
     */
    public void invalidate() {
        synchronized (cache) {
            cache.clear();
        }
    }
    
    /**
     * Returns the number of cached query results.
     * Useful for monitoring and debugging.
     *
     * @return the number of cached entries
     */
    public int size() {
        synchronized (cache) {
            return cache.size();
        }
    }
    
    /**
     * Returns the maximum cache size.
     *
     * @return the maximum number of entries this cache can hold
     */
    public int getMaxSize() {
        return maxSize;
    }
}
