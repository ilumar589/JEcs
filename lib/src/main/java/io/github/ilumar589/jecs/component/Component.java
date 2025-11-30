package io.github.ilumar589.jecs.component;

/**
 * Marker interface for all ECS components.
 * Components are pure data containers (typically records) that hold entity state.
 * All component implementations should implement this interface.
 * 
 * <h2>Design Philosophy</h2>
 * Components in this ECS framework are designed to be:
 * <ul>
 *   <li><b>Immutable:</b> Prefer immutable records for thread safety and predictability</li>
 *   <li><b>Value-like:</b> Components should behave like values, not entities</li>
 *   <li><b>Simple:</b> Components should contain only data, no behavior beyond accessors</li>
 * </ul>
 * 
 * <h2>Project Valhalla Migration Path</h2>
 * This interface and its implementations are designed with Project Valhalla's 
 * value types in mind. When value types become available:
 * 
 * <h3>Current (Pre-Valhalla)</h3>
 * <pre>{@code
 * public record Position(float x, float y, float z) implements Component {}
 * }</pre>
 * 
 * <h3>Future (Post-Valhalla, hypothetical syntax)</h3>
 * <pre>{@code
 * public value record Position(float x, float y, float z) implements Component {}
 * // or
 * value class Position implements Component {
 *     float x, y, z;
 * }
 * }</pre>
 * 
 * <h2>Benefits of Value Types for Components</h2>
 * <ul>
 *   <li><b>Flattened storage:</b> Components can be stored inline in arrays without
 *       object headers or reference indirection</li>
 *   <li><b>Cache efficiency:</b> Sequential component access will be even more
 *       cache-friendly with true contiguous memory layout</li>
 *   <li><b>Reduced allocations:</b> Value types don't require heap allocation,
 *       reducing GC pressure in high-frequency component updates</li>
 *   <li><b>Identity-free semantics:</b> Components are compared by value, matching
 *       their semantic purpose as pure data containers</li>
 * </ul>
 * 
 * <h2>Best Practices for Valhalla Readiness</h2>
 * <ol>
 *   <li>Use Java records for component implementations</li>
 *   <li>Keep components immutable - create new instances instead of modifying</li>
 *   <li>Use primitive fields where possible (float, int, double, long)</li>
 *   <li>Avoid storing references to mutable objects within components</li>
 *   <li>Keep component size small (ideally fitting within 1-2 cache lines)</li>
 * </ol>
 * 
 * @see io.github.ilumar589.jecs.world.ComponentStore
 * @see io.github.ilumar589.jecs.world.Archetype
 */
public interface Component {
}
