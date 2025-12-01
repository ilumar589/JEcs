package io.github.ilumar589.jecs.query;

/**
 * A type-safe tuple of two components from the same entity.
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * List<ComponentTuple2<Position, Velocity>> results = world.query()
 *     .with(Position.class, Velocity.class)
 *     .results();
 *     
 * for (var tuple : results) {
 *     Position pos = tuple.first();
 *     Velocity vel = tuple.second();
 * }
 * }</pre>
 *
 * @param <A> the type of the first component
 * @param <B> the type of the second component
 * @param first the first component
 * @param second the second component
 */
public record ComponentTuple2<A, B>(A first, B second) {
}
