package io.github.ilumar589.jecs.query;

/**
 * A wrapper for a read-only component.
 * This indicates that the component should not be modified.
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * world.componentQuery()
 *     .withReadOnly(Velocity.class)
 *     .forEachWithAccess(Velocity.class, (ReadOnly<Velocity> vel) -> {
 *         // vel.get() returns the Velocity, but you shouldn't modify it
 *         float speed = vel.get().dx();
 *     });
 * }</pre>
 *
 * @param <T> the component type
 * @param value the wrapped component value
 */
public record ReadOnly<T>(T value) {
    /**
     * Gets the wrapped component value.
     * The component should be treated as read-only.
     *
     * @return the component value
     */
    public T get() {
        return value;
    }
}
