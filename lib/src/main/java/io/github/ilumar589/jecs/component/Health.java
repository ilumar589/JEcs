package io.github.ilumar589.jecs.component;

/**
 * Example component representing health status.
 *
 * @param current the current health value
 * @param max the maximum health value
 */
public record Health(int current, int max) implements Component {
}
