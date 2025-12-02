package io.github.ilumar589.jecs.query;

import io.github.ilumar589.jecs.world.Archetype;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache for query results.
 * Maps query signatures (cache keys) to lists of matching archetypes.
 * 
 * <h2>Cache Invalidation</h2>
 * The cache is invalidated (cleared) when new archetypes are created,
 * as new archetypes may match existing queries.
 * 
 * <h2>Thread Safety</h2>
 * This cache uses {@link ConcurrentHashMap} for thread-safe operations.
 * Cache lookups and insertions are lock-free for concurrent query execution.
 * 
 * <h2>Performance Benefits</h2>
 * <ul>
 *   <li>Eliminates repeated archetype filtering for identical queries</li>
 *   <li>O(1) cache lookup vs O(archetypes × types) filtering</li>
 *   <li>Particularly beneficial for game loops with repeated queries</li>
 * </ul>
 * 
 * <h2>Memory Considerations</h2>
 * The cache stores references to archetype lists, not copies.
 * Memory usage is proportional to the number of unique query patterns.
 */
public final class QueryCache {
    
    private final Map<QueryCacheKey, List<Archetype>> cache;
    
    /**
     * Creates a new empty query cache.
     */
    public QueryCache() {
        this.cache = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets cached matching archetypes for a query, or null if not cached.
     *
     * @param key the query cache key
     * @return the cached list of matching archetypes, or null if not in cache
     */
    public List<Archetype> get(QueryCacheKey key) {
        return cache.get(key);
    }
    
    /**
     * Puts matching archetypes into the cache for a query.
     *
     * @param key the query cache key
     * @param archetypes the list of matching archetypes
     */
    public void put(QueryCacheKey key, List<Archetype> archetypes) {
        cache.put(key, archetypes);
    }
    
    /**
     * Invalidates (clears) the entire cache.
     * This should be called when new archetypes are created,
     * as they may match existing query patterns.
     */
    public void invalidate() {
        cache.clear();
    }
    
    /**
     * Returns the number of cached query results.
     * Useful for monitoring and debugging.
     *
     * @return the number of cached entries
     */
    public int size() {
        return cache.size();
    }
}
