/**
 * Core ECS world management including archetypes and component storage.
 * 
 * <p>Key classes:
 * <ul>
 *   <li>{@link io.github.ilumar589.jecs.world.EcsWorld} - Main entry point for entity/component management</li>
 *   <li>{@link io.github.ilumar589.jecs.world.Archetype} - Groups entities with same component types using primitive decomposition</li>
 *   <li>{@link io.github.ilumar589.jecs.world.ComponentReader} - Read-only access to components (no memory barriers)</li>
 *   <li>{@link io.github.ilumar589.jecs.world.ComponentWriter} - Mutable access to components (release semantics)</li>
 *   <li>{@link io.github.ilumar589.jecs.world.GlobalArray} - Sealed interface for type-safe primitive array storage with VarHandles</li>
 *   <li>{@link io.github.ilumar589.jecs.world.FieldColumn} - Field metadata and array offset tracking</li>
 *   <li>{@link io.github.ilumar589.jecs.world.FieldKey} - Unique field identification across component types</li>
 * </ul>
 * 
 * <h2>Null Safety</h2>
 * All types are non-null by default unless annotated with {@code @Nullable}.
 */
@NullMarked
package io.github.ilumar589.jecs.world;

import org.jspecify.annotations.NullMarked;
