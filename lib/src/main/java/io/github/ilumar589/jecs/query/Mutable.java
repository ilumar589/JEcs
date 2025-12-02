package io.github.ilumar589.jecs.query;

import io.github.ilumar589.jecs.world.ComponentWriter;

import java.util.function.Function;

/**
 * Mutable wrapper with direct primitive array access.
 * Writes immediately to primitive arrays, no staging required.
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * world.componentQuery()
 *     .withMutable(Position.class)
 *     .withReadOnly(Velocity.class)
 *     .forEach(wrappers -> {
 *         Mutable<Position> pos = (Mutable<Position>) wrappers[0];
 *         ReadOnly<Velocity> vel = (ReadOnly<Velocity>) wrappers[1];
 *         
 *         // Update position based on velocity - writes IMMEDIATELY to primitives
 *         pos.set(new Position(
 *             pos.get().x() + vel.get().dx(),
 *             pos.get().y() + vel.get().dy(),
 *             pos.get().z() + vel.get().dz()
 *         ));
 *     }, Position.class, Velocity.class);
 * }</pre>
 *
 * <h2>Valhalla Readiness</h2>
 * This class is designed to become a value class when Project Valhalla lands.
 * No code changes will be required for migration.
 *
 * @param <T> the component type
 */
public final class Mutable<T> implements ComponentWrapper<T> {
    private final ComponentWriter<T> writer;
    private final int entityIndex;
    private T cachedValue;
    private boolean valueCached;

    /**
     * Creates a new Mutable wrapper with direct primitive array access.
     * Package-private constructor - called by ComponentQuery.
     *
     * @param writer the component writer for direct array access
     * @param entityIndex the index of the entity in the archetype
     */
    Mutable(ComponentWriter<T> writer, int entityIndex) {
        this.writer = writer;
        this.entityIndex = entityIndex;
        this.valueCached = false;
    }

    /**
     * Gets the current component value (lazy reconstruction from primitives).
     *
     * @return the component value
     */
    @Override
    public T get() {
        if (!valueCached) {
            cachedValue = writer.read(entityIndex);
            valueCached = true;
        }
        return cachedValue;
    }

    /**
     * Sets a new component value - writes IMMEDIATELY to primitive arrays.
     * No staging or write-back is needed.
     *
     * @param newValue the new component value
     */
    public void set(T newValue) {
        writer.write(entityIndex, newValue);
        cachedValue = newValue;
        valueCached = true;
    }

    /**
     * Updates the component using a transformation function.
     * Writes IMMEDIATELY to primitive arrays.
     *
     * @param transformer the function to transform the component
     */
    public void update(Function<T, T> transformer) {
        T current = get();
        T updated = transformer.apply(current);
        set(updated);
    }
}
