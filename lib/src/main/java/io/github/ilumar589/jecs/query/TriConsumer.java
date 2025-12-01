package io.github.ilumar589.jecs.query;

/**
 * A functional interface that accepts three arguments.
 * Used for processing three components in a query result.
 *
 * @param <A> the type of the first argument
 * @param <B> the type of the second argument
 * @param <C> the type of the third argument
 */
@FunctionalInterface
public interface TriConsumer<A, B, C> {
    
    /**
     * Performs this operation on the given arguments.
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     */
    void accept(A a, B b, C c);
}
