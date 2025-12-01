package io.github.ilumar589.jecs.query;

/**
 * A type-safe tuple of four components from the same entity.
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * List<ComponentTuple4<Position, Velocity, Health, Weapon>> results = world.query()
 *     .with(Position.class, Velocity.class, Health.class, Weapon.class)
 *     .results();
 *     
 * for (var tuple : results) {
 *     Position pos = tuple.first();
 *     Velocity vel = tuple.second();
 *     Health health = tuple.third();
 *     Weapon weapon = tuple.fourth();
 * }
 * }</pre>
 *
 * @param <A> the type of the first component
 * @param <B> the type of the second component
 * @param <C> the type of the third component
 * @param <D> the type of the fourth component
 * @param first the first component
 * @param second the second component
 * @param third the third component
 * @param fourth the fourth component
 */
public record ComponentTuple4<A, B, C, D>(A first, B second, C third, D fourth) {
}
