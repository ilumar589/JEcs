package io.github.ilumar589.jecs.world;

/**
 * Provides mutable access to components stored in a PrimitiveArchetype.
 * Uses VarHandle.setRelease() for safe writes with proper memory barriers.
 * 
 * <h2>Memory Semantics</h2>
 * <ul>
 *   <li>Write operations use release semantics</li>
 *   <li>Ensures visibility of writes to other threads that perform acquire reads</li>
 *   <li>Suitable for safe publication in concurrent ECS systems</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ComponentWriter<Position> posWriter = archetype.getWriter(Position.class);
 * ComponentReader<Velocity> velReader = archetype.getReader(Velocity.class);
 * 
 * for (int i = 0; i < archetype.size(); i++) {
 *     Velocity vel = velReader.read(i);
 *     Position pos = archetype.getReader(Position.class).read(i);
 *     posWriter.write(i, new Position(
 *         pos.x() + vel.dx() * dt,
 *         pos.y() + vel.dy() * dt,
 *         pos.z() + vel.dz() * dt
 *     ));
 * }
 * }</pre>
 * 
 * <h2>Performance Considerations</h2>
 * While writers provide stronger guarantees than readers, they are still
 * highly performant due to:
 * <ul>
 *   <li>VarHandle's optimized implementation</li>
 *   <li>Sequential memory access patterns</li>
 *   <li>No object allocation for primitive fields</li>
 * </ul>
 *
 * @param <T> the component type
 */
public interface ComponentWriter<T> {
    
    /**
     * Writes a component at the specified entity index.
     * The component is decomposed into its primitive fields and written
     * to the appropriate typed arrays using release semantics.
     *
     * @param entityIndex the index of the entity
     * @param component the component to write
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    void write(int entityIndex, T component);
    
    /**
     * Returns the number of entities in this writer's archetype.
     *
     * @return the number of entities
     */
    int size();
}
