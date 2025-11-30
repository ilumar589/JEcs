package io.github.ilumar589.jecs.component;

/**
 * Example component representing health status.
 * 
 * <h2>Valhalla-Ready Design</h2>
 * This component uses primitive int fields which will benefit from value type
 * optimizations when Project Valhalla lands:
 * <ul>
 *   <li>No interface overhead - pure record that can become a value type</li>
 *   <li>Both fields are primitives (no boxing)</li>
 *   <li>Total size is 8 bytes (2 × 4 bytes for ints)</li>
 *   <li>Fits well within a single cache line</li>
 * </ul>
 *
 * @param current the current health value
 * @param max the maximum health value
 */
public record Health(int current, int max) {
}
