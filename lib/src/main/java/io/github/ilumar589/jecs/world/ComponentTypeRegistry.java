package io.github.ilumar589.jecs.world;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registry that assigns each component type a unique bit index.
 * Enables O(1) bitwise operations for archetype matching instead of O(n×m) set operations.
 * 
 * <h2>Performance Benefits</h2>
 * <ul>
 *   <li>Archetype matching becomes O(1) bitwise operations</li>
 *   <li>{@code (archetypeBits & requiredBits) == requiredBits} for inclusion check</li>
 *   <li>{@code (archetypeBits & excludedBits) == 0} for exclusion check</li>
 *   <li>Cache-friendly compact representation</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * This registry is thread-safe. Bit index assignments use atomic operations
 * and the mapping is stored in a ConcurrentHashMap.
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ComponentTypeRegistry registry = new ComponentTypeRegistry();
 * int positionBit = registry.getBitIndex(Position.class);
 * int velocityBit = registry.getBitIndex(Velocity.class);
 * 
 * BitSet archetypeBits = registry.toBitSet(Set.of(Position.class, Velocity.class));
 * BitSet requiredBits = registry.toBitSet(Set.of(Position.class));
 * 
 * // Check if archetype has required components
 * BitSet temp = (BitSet) requiredBits.clone();
 * temp.and(archetypeBits);
 * boolean matches = temp.equals(requiredBits);
 * }</pre>
 */
public final class ComponentTypeRegistry {
    
    private final Map<Class<?>, Integer> typeToIndex;
    private final AtomicInteger nextIndex;
    
    /**
     * Creates a new empty component type registry.
     */
    public ComponentTypeRegistry() {
        this.typeToIndex = new ConcurrentHashMap<>();
        this.nextIndex = new AtomicInteger(0);
    }
    
    /**
     * Gets or assigns a bit index for the given component type.
     * Thread-safe - concurrent calls for the same type will return the same index.
     *
     * @param componentType the component class
     * @return the unique bit index for this type
     */
    public int getBitIndex(Class<?> componentType) {
        return typeToIndex.computeIfAbsent(componentType, k -> nextIndex.getAndIncrement());
    }
    
    /**
     * Converts a set of component types to a BitSet.
     * Each bit position corresponds to a component type's index.
     *
     * @param types the set of component types
     * @return a BitSet with bits set for each type
     */
    public BitSet toBitSet(Set<Class<?>> types) {
        BitSet bits = new BitSet();
        for (Class<?> type : types) {
            bits.set(getBitIndex(type));
        }
        return bits;
    }
    
    /**
     * Checks if an archetype's component types contain all required types.
     * This is an O(1) bitwise operation.
     *
     * @param archetypeBits the archetype's component bits
     * @param requiredBits the required component bits
     * @return true if all required bits are present in archetype bits
     */
    public static boolean containsAll(BitSet archetypeBits, BitSet requiredBits) {
        if (requiredBits.isEmpty()) {
            return true;
        }
        // (archetypeBits & requiredBits) == requiredBits
        BitSet intersection = (BitSet) archetypeBits.clone();
        intersection.and(requiredBits);
        return intersection.equals(requiredBits);
    }
    
    /**
     * Checks if an archetype's component types contain any excluded types.
     * This is an O(1) bitwise operation.
     *
     * @param archetypeBits the archetype's component bits
     * @param excludedBits the excluded component bits
     * @return true if any excluded bit is present in archetype bits
     */
    public static boolean intersects(BitSet archetypeBits, BitSet excludedBits) {
        if (excludedBits.isEmpty()) {
            return false;
        }
        // (archetypeBits & excludedBits) != 0
        return archetypeBits.intersects(excludedBits);
    }
    
    /**
     * Returns the number of registered component types.
     *
     * @return the count of registered types
     */
    public int size() {
        return nextIndex.get();
    }
    
    /**
     * Checks if a component type has been registered.
     *
     * @param componentType the component class
     * @return true if the type has a bit index assigned
     */
    public boolean isRegistered(Class<?> componentType) {
        return typeToIndex.containsKey(componentType);
    }
}
