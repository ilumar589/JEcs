/**
 * Example component implementations for the ECS framework.
 * 
 * <p>This package contains example components that demonstrate best practices:
 * <ul>
 *   <li>{@link io.github.ilumar589.jecs.component.Position} - 3D position with float coordinates</li>
 *   <li>{@link io.github.ilumar589.jecs.component.Velocity} - 3D velocity vector</li>
 *   <li>{@link io.github.ilumar589.jecs.component.Health} - Health points with current/max values</li>
 * </ul>
 * 
 * <h2>Component Best Practices</h2>
 * Components should be:
 * <ul>
 *   <li>Immutable records with primitive fields</li>
 *   <li>Small (ideally fitting in 1-2 cache lines)</li>
 *   <li>Free of reference types when possible</li>
 * </ul>
 * 
 * <h2>Null Safety</h2>
 * All types are non-null by default unless annotated with {@code @Nullable}.
 */
@NullMarked
package io.github.ilumar589.jecs.component;

import org.jspecify.annotations.NullMarked;
