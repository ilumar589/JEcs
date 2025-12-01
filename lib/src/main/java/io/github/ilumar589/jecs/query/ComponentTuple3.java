package io.github.ilumar589.jecs.query;

/**
 * A type-safe tuple of three components from the same entity.
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * List<ComponentTuple3<Position, Velocity, Health>> results = world.query()
 *     .with(Position.class, Velocity.class, Health.class)
 *     .results();
 *     
 * for (var tuple : results) {
 *     Position pos = tuple.first();
 *     Velocity vel = tuple.second();
 *     Health health = tuple.third();
 * }
 * }</pre>
 *
 * @param <A> the type of the first component
 * @param <B> the type of the second component
 * @param <C> the type of the third component
 * @param first the first component
 * @param second the second component
 * @param third the third component
 */
public record ComponentTuple3<A, B, C>(A first, B second, C third) {
}
