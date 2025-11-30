package io.github.ilumar589.jecs.component;

/**
 * Example component representing a position in 3D space.
 *
 * @param x the x coordinate
 * @param y the y coordinate
 * @param z the z coordinate
 */
public record Position(float x, float y, float z) implements Component {
}
