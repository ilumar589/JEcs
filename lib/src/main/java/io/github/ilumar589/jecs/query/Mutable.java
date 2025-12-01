package io.github.ilumar589.jecs.query;

import java.util.function.Function;

/**
 * A wrapper for a mutable component that tracks modifications.
 * Provides methods to read and update the component value.
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * world.componentQuery()
 *     .withReadOnly(Velocity.class)
 *     .withMutable(Position.class)
 *     .forEachWithAccess(Position.class, Velocity.class, 
 *         (Mutable<Position> pos, ReadOnly<Velocity> vel) -> {
 *             // Update position based on velocity
 *             pos.set(new Position(
 *                 pos.get().x() + vel.get().dx(),
 *                 pos.get().y() + vel.get().dy(),
 *                 pos.get().z() + vel.get().dz()
 *             ));
 *         });
 * }</pre>
 *
 * @param <T> the component type
 */
public final class Mutable<T> {
    private T value;
    private boolean modified;

    /**
     * Creates a new Mutable wrapper.
     *
     * @param value the initial component value
     */
    public Mutable(T value) {
        this.value = value;
        this.modified = false;
    }

    /**
     * Gets the current component value.
     *
     * @return the component value
     */
    public T get() {
        return value;
    }

    /**
     * Sets a new component value.
     *
     * @param newValue the new component value
     */
    public void set(T newValue) {
        this.value = newValue;
        this.modified = true;
    }

    /**
     * Updates the component using a transformation function.
     *
     * @param transformer the function to transform the component
     */
    public void update(Function<T, T> transformer) {
        this.value = transformer.apply(this.value);
        this.modified = true;
    }

    /**
     * Checks if the component has been modified.
     *
     * @return true if the component was modified
     */
    public boolean isModified() {
        return modified;
    }
}
