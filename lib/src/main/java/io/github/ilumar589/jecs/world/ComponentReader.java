package io.github.ilumar589.jecs.world;

import java.util.function.Consumer;

/**
 * Provides read-only access to components stored in a PrimitiveArchetype.
 * Uses VarHandle.getPlain() for maximum performance with no memory barriers.
 * 
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li>No memory barriers - fastest possible read path</li>
 *   <li>Sequential access patterns enable CPU prefetching</li>
 *   <li>No GC pressure for primitive fields</li>
 *   <li>Enables auto-vectorization for primitive loops</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ComponentReader<Position> posReader = archetype.getReader(Position.class);
 * for (int i = 0; i < archetype.size(); i++) {
 *     Position pos = posReader.read(i);
 *     // Read-only processing
 * }
 * }</pre>
 * 
 * <h2>Thread Safety</h2>
 * Readers use plain reads which do not guarantee visibility of writes from
 * other threads. For concurrent access, ensure proper synchronization at
 * a higher level (e.g., between systems).
 *
 * @param <T> the component type
 */
public interface ComponentReader<T> {
    
    /**
     * Reads a component at the specified entity index.
     * Uses plain read semantics (no memory barriers).
     *
     * @param entityIndex the index of the entity
     * @return the component at the specified index
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    T read(int entityIndex);
    
    /**
     * Returns the number of entities in this reader's archetype.
     *
     * @return the number of entities
     */
    int size();
    
    /**
     * Processes a batch of components sequentially.
     * This method is optimized for cache-friendly iteration.
     *
     * @param startIndex the starting entity index
     * @param count the number of entities to process
     * @param processor the consumer to process each component
     * @throws IndexOutOfBoundsException if the range exceeds bounds
     */
    default void readBatch(int startIndex, int count, Consumer<T> processor) {
        int end = startIndex + count;
        for (int i = startIndex; i < end; i++) {
            processor.accept(read(i));
        }
    }
    
    /**
     * Processes all components in this reader.
     *
     * @param processor the consumer to process each component
     */
    default void readAll(Consumer<T> processor) {
        readBatch(0, size(), processor);
    }
}
