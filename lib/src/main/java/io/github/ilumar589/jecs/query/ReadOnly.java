package io.github.ilumar589.jecs.query;

import io.github.ilumar589.jecs.world.ComponentReader;

import java.util.function.Function;

/**
 * Read-only wrapper that actively prevents mutations.
 * Throws exceptions if user tries to modify the component.
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
 *         // vel.get() returns the Velocity, but you cannot modify it
 *         float speed = vel.get().dx();
 *         
 *         // Trying this throws UnsupportedOperationException:
 *         // vel.set(new Velocity(0, 0, 0));  // THROWS!
 *     }, Position.class, Velocity.class);
 * }</pre>
 *
 * <h2>Valhalla Readiness</h2>
 * This class is designed to become a value class when Project Valhalla lands.
 * No code changes will be required for migration.
 *
 * @param <T> the component type
 */
public final class ReadOnly<T> implements ComponentWrapper<T> {
    private ComponentReader<T> reader;
    private int entityIndex;
    private T cachedValue;
    private boolean valueCached;

    /**
     * Creates a new ReadOnly wrapper.
     * Package-private constructor - called by ComponentQuery.
     *
     * @param reader the component reader for direct array access
     * @param entityIndex the index of the entity in the archetype
     */
    ReadOnly(ComponentReader<T> reader, int entityIndex) {
        this.reader = reader;
        this.entityIndex = entityIndex;
        this.valueCached = false;
    }

    /**
     * Updates this wrapper to point to a different entity index.
     * This method is used internally to reuse wrapper instances across entity iterations,
     * reducing allocation overhead from O(entities × components) to O(components).
     * Package-private - called by ComponentQuery.
     *
     * @param newReader the component reader (may be from a different archetype)
     * @param newEntityIndex the new entity index to point to
     */
    void reset(ComponentReader<T> newReader, int newEntityIndex) {
        this.reader = newReader;
        this.entityIndex = newEntityIndex;
        this.valueCached = false;
        this.cachedValue = null;
    }

    /**
     * Gets the wrapped component value (lazy reconstruction with plain reads for max performance).
     * The component should be treated as read-only.
     *
     * @return the component value
     */
    @Override
    public T get() {
        if (!valueCached) {
            cachedValue = reader.read(entityIndex);
            valueCached = true;
        }
        return cachedValue;
    }

    /**
     * Throws UnsupportedOperationException - this component is read-only!
     * Use withMutable() instead of withReadOnly() if you need to modify this component.
     *
     * @param newValue the new value (ignored)
     * @throws UnsupportedOperationException always
     */
    public void set(T newValue) {
        throw createReadOnlyException();
    }

    /**
     * Throws UnsupportedOperationException - this component is read-only!
     * Use withMutable() instead of withReadOnly() if you need to modify this component.
     *
     * @param transformer the transformer function (ignored)
     * @throws UnsupportedOperationException always
     */
    public void update(Function<T, T> transformer) {
        throw createReadOnlyException();
    }

    /**
     * Creates an UnsupportedOperationException with a helpful error message.
     */
    private UnsupportedOperationException createReadOnlyException() {
        String typeName = valueCached && cachedValue != null 
            ? cachedValue.getClass().getSimpleName() 
            : "unknown";
        return new UnsupportedOperationException(
            "Cannot modify read-only component of type: " + typeName + 
            ". Use withMutable() instead of withReadOnly() if you need to modify this component.");
    }
}
