package io.github.ilumar589.jecs.query;

/**
 * Base sealed interface for component wrappers.
 * Allows generic handling of {@link Mutable} and {@link ReadOnly} wrappers.
 * 
 * <h2>Valhalla Readiness</h2>
 * This interface is designed to be compatible with Project Valhalla.
 * When value types become available, implementations can become value classes
 * while maintaining the same API.
 *
 * @param <T> the component type
 */
public sealed interface ComponentWrapper<T> permits Mutable, ReadOnly {
    
    /**
     * Gets the current component value.
     *
     * @return the component value
     */
    T get();
}
