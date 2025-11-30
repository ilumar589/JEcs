package io.github.ilumar589.jecs.entity;

/**
 * Represents an entity in the ECS world.
 * An entity is essentially just an identifier (ID) with an optional generation field for future safety.
 *
 * <h2>Value-Based Design</h2>
 * This record is designed as a value-based class:
 * <ul>
 *   <li>Instances are immutable and final</li>
 *   <li>Identity is based entirely on {@link #id()} and {@link #generation()}</li>
 *   <li>No synchronization should be performed using Entity instances as locks</li>
 *   <li>Comparison should use {@link #equals(Object)}, not identity (==)</li>
 * </ul>
 * 
 * <h2>Project Valhalla Migration Path</h2>
 * When Project Valhalla's value types become available, this record is a prime
 * candidate for conversion to a value class:
 * 
 * <h3>Current (Pre-Valhalla)</h3>
 * <pre>{@code
 * public record Entity(int id, int generation) {}
 * }</pre>
 * 
 * <h3>Future (Post-Valhalla, hypothetical syntax)</h3>
 * <pre>{@code
 * public value record Entity(int id, int generation) {}
 * // or as a primitive class:
 * primitive class Entity {
 *     int id;
 *     int generation;
 * }
 * }</pre>
 * 
 * <h2>Benefits of Value Type Conversion</h2>
 * <ul>
 *   <li><b>Zero allocation:</b> Entity values won't require heap allocation</li>
 *   <li><b>Flattened arrays:</b> {@code Entity[]} will store id/generation pairs
 *       inline without object headers (16+ bytes each saved)</li>
 *   <li><b>Pass-by-value:</b> Method calls won't require reference indirection</li>
 *   <li><b>Cache efficiency:</b> Entity arrays become contiguous int[] equivalents</li>
 * </ul>
 * 
 * <h2>Generation Field Purpose</h2>
 * The generation field enables entity handle reuse:
 * <ul>
 *   <li>When an entity is destroyed, its ID can be recycled</li>
 *   <li>The generation is incremented on reuse to detect stale references</li>
 *   <li>A reference with matching ID but wrong generation is invalid</li>
 * </ul>
 *
 * @param id the unique identifier for this entity
 * @param generation the generation/version number, incremented when an ID is reused
 * 
 * @jdk.internal.ValueBased
 */
public record Entity(int id, int generation) {
    /**
     * Creates a new entity with generation 0.
     *
     * @param id the unique identifier for this entity
     */
    public Entity(int id) {
        this(id, 0);
    }
}
