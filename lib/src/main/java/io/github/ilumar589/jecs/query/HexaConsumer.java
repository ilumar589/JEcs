package io.github.ilumar589.jecs.query;

/**
 * A functional interface that accepts six arguments.
 * Used for processing six components in a query result.
 *
 * @param <A> the type of the first argument
 * @param <B> the type of the second argument
 * @param <C> the type of the third argument
 * @param <D> the type of the fourth argument
 * @param <E> the type of the fifth argument
 * @param <F> the type of the sixth argument
 */
@FunctionalInterface
public interface HexaConsumer<A, B, C, D, E, F> {
    
    /**
     * Performs this operation on the given arguments.
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     * @param d the fourth input argument
     * @param e the fifth input argument
     * @param f the sixth input argument
     */
    void accept(A a, B b, C c, D d, E e, F f);
}
