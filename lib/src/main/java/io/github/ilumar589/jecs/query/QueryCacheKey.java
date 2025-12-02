package io.github.ilumar589.jecs.query;

import java.util.Arrays;
import java.util.Set;

/**
 * Immutable key for caching query results.
 * Uses sorted arrays for efficient equality checks and hash computation.
 * 
 * <h2>Cache Key Components</h2>
 * The key is composed of:
 * <ul>
 *   <li>Included component types (from with/withMutable/withReadOnly)</li>
 *   <li>Excluded component types (from without)</li>
 *   <li>Additional required types (from forEach parameter)</li>
 * </ul>
 * 
 * <h2>Performance</h2>
 * <ul>
 *   <li>Hash code is pre-computed for fast HashMap lookups</li>
 *   <li>Sorted arrays enable O(n) equality checks</li>
 *   <li>Immutable design allows safe caching without defensive copies</li>
 * </ul>
 */
public final class QueryCacheKey {
    
    private final Class<?>[] includedTypes;
    private final Class<?>[] excludedTypes;
    private final Class<?>[] additionalRequired;
    private final int hash;
    
    /**
     * Creates a new query cache key.
     *
     * @param includedTypes the set of included component types
     * @param excludedTypes the set of excluded component types
     * @param additionalRequired the set of additional required types (from forEach)
     */
    public QueryCacheKey(Set<Class<?>> includedTypes, Set<Class<?>> excludedTypes, 
                          Set<Class<?>> additionalRequired) {
        this.includedTypes = toSortedArray(includedTypes);
        this.excludedTypes = toSortedArray(excludedTypes);
        this.additionalRequired = toSortedArray(additionalRequired);
        
        // Pre-compute hash for fast lookups
        int h = Arrays.hashCode(this.includedTypes);
        h = 31 * h + Arrays.hashCode(this.excludedTypes);
        h = 31 * h + Arrays.hashCode(this.additionalRequired);
        this.hash = h;
    }
    
    private static Class<?>[] toSortedArray(Set<Class<?>> types) {
        if (types.isEmpty()) {
            return new Class<?>[0];
        }
        Class<?>[] array = types.toArray(new Class<?>[0]);
        Arrays.sort(array, (a, b) -> a.getName().compareTo(b.getName()));
        return array;
    }
    
    @Override
    public int hashCode() {
        return hash;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryCacheKey that = (QueryCacheKey) o;
        return Arrays.equals(includedTypes, that.includedTypes)
            && Arrays.equals(excludedTypes, that.excludedTypes)
            && Arrays.equals(additionalRequired, that.additionalRequired);
    }
    
    @Override
    public String toString() {
        return "QueryCacheKey{" +
                "included=" + Arrays.toString(includedTypes) +
                ", excluded=" + Arrays.toString(excludedTypes) +
                ", additional=" + Arrays.toString(additionalRequired) +
                '}';
    }
}
