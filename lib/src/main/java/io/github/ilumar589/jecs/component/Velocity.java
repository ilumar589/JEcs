package io.github.ilumar589.jecs.component;

/**
 * Example component representing velocity in 3D space.
 * 
 * <h2>Valhalla-Ready Design</h2>
 * This component uses primitive float fields which will benefit from value type
 * optimizations when Project Valhalla lands:
 * <ul>
 *   <li>All three velocity components are primitives (no boxing)</li>
 *   <li>Total size is 12 bytes (3 × 4 bytes for floats)</li>
 *   <li>Fits well within a single cache line (typically 64 bytes)</li>
 *   <li>Ideal candidate for flattened array storage</li>
 * </ul>
 * 
 * <h2>Common Usage Pattern</h2>
 * Velocity is typically used with Position for movement systems:
 * <pre>{@code
 * // Movement system - iterating both Position and Velocity
 * for (Entity entity : world.query(Position.class, Velocity.class)) {
 *     Position pos = world.getComponent(entity, Position.class);
 *     Velocity vel = world.getComponent(entity, Velocity.class);
 *     world.setComponent(entity, new Position(
 *         pos.x() + vel.dx() * dt,
 *         pos.y() + vel.dy() * dt,
 *         pos.z() + vel.dz() * dt
 *     ));
 * }
 * }</pre>
 *
 * @param dx the velocity along the x axis
 * @param dy the velocity along the y axis
 * @param dz the velocity along the z axis
 */
public record Velocity(float dx, float dy, float dz) implements Component {
}
