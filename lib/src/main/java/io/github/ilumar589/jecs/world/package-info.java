/**
 * Core ECS world management including archetypes and component storage.
 * 
 * <p>Key classes:
 * <ul>
 *   <li>{@link io.github.ilumar589.jecs.world.EcsWorld} - Main entry point for entity/component management</li>
 *   <li>{@link io.github.ilumar589.jecs.world.Archetype} - Groups entities with same component types</li>
 *   <li>{@link io.github.ilumar589.jecs.world.ComponentStore} - Array-backed component storage</li>
 * </ul>
 * 
 * <h2>Null Safety</h2>
 * All types are non-null by default unless annotated with {@code @Nullable}.
 */
@NullMarked
package io.github.ilumar589.jecs.world;

import org.jspecify.annotations.NullMarked;
