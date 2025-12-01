package io.github.ilumar589.jecs.query;

/**
 * A functional interface that accepts four arguments.
 * Used for processing four components in a query result.
 *
 * @param <A> the type of the first argument
 * @param <B> the type of the second argument
 * @param <C> the type of the third argument
 * @param <D> the type of the fourth argument
 */
@FunctionalInterface
public interface QuadConsumer<A, B, C, D> {
    
    /**
     * Performs this operation on the given arguments.
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     * @param d the fourth input argument
     */
    void accept(A a, B b, C c, D d);
}
