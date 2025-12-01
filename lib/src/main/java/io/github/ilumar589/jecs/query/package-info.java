/**
 * Component-focused query API for the JEcs ECS framework.
 *
 * <h2>Overview</h2>
 * This package provides a fluent, component-focused API for querying entities
 * without exposing internal implementation details like Entity or Archetype classes.
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link io.github.ilumar589.jecs.query.ComponentQuery} - Fluent builder for queries</li>
 *   <li>{@link io.github.ilumar589.jecs.query.ComponentTuple2} - Type-safe pair of components</li>
 *   <li>{@link io.github.ilumar589.jecs.query.ComponentTuple3} - Type-safe triple of components</li>
 *   <li>{@link io.github.ilumar589.jecs.query.ComponentTuple4} - Type-safe quadruple of components</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Query with inclusion and exclusion
 * world.query()
 *     .with(Position.class, Velocity.class)
 *     .without(Dead.class)
 *     .forEach((pos, vel) -> {
 *         // Process components directly
 *     });
 * }</pre>
 */
package io.github.ilumar589.jecs.query;
