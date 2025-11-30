package io.github.ilumar589.jecs.component;

/**
 * Example component representing a position in 3D space.
 * 
 * <h2>Valhalla-Ready Design</h2>
 * This component uses primitive float fields which will benefit from value type
 * optimizations when Project Valhalla lands:
 * <ul>
 *   <li>All three coordinates are primitives (no boxing)</li>
 *   <li>Total size is 12 bytes (3 × 4 bytes for floats)</li>
 *   <li>Fits well within a single cache line (typically 64 bytes)</li>
 *   <li>Ideal candidate for flattened array storage</li>
 * </ul>
 * 
 * <h2>Cache-Friendly Iteration</h2>
 * When iterating over Position components in an archetype:
 * <pre>{@code
 * ComponentStore<Position> store = archetype.getComponentStore(Position.class);
 * Object[] data = store.getRawData();
 * for (int i = 0; i < store.size(); i++) {
 *     Position pos = (Position) data[i];
 *     // Sequential access maximizes cache hits
 * }
 * }</pre>
 *
 * @param x the x coordinate
 * @param y the y coordinate
 * @param z the z coordinate
 */
public record Position(float x, float y, float z) implements Component {
}
