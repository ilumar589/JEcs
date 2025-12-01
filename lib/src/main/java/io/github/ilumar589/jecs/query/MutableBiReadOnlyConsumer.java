package io.github.ilumar589.jecs.query;

/**
 * A functional interface for consuming one mutable and two read-only components.
 * Used for iteration patterns where one component needs modification based on two others.
 *
 * @param <A> the type of the mutable component
 * @param <B> the type of the first read-only component
 * @param <C> the type of the second read-only component
 */
@FunctionalInterface
public interface MutableBiReadOnlyConsumer<A, B, C> {
    
    /**
     * Performs this operation on the given arguments.
     *
     * @param mutable the mutable component wrapper
     * @param readOnly1 the first read-only component wrapper
     * @param readOnly2 the second read-only component wrapper
     */
    void accept(Mutable<A> mutable, ReadOnly<B> readOnly1, ReadOnly<C> readOnly2);
}
