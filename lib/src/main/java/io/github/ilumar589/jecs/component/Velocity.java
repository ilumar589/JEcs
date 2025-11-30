package io.github.ilumar589.jecs.component;

/**
 * Example component representing velocity in 3D space.
 *
 * @param dx the velocity along the x axis
 * @param dy the velocity along the y axis
 * @param dz the velocity along the z axis
 */
public record Velocity(float dx, float dy, float dz) implements Component {
}
