/**
 * Entity representation for the ECS framework.
 * 
 * <p>The {@link io.github.ilumar589.jecs.entity.Entity} record serves as a unique
 * identifier for game objects in the ECS world. It uses a generation field for
 * safe entity handle reuse.
 * 
 * <h2>Null Safety</h2>
 * All types are non-null by default unless annotated with {@code @Nullable}.
 */
@NullMarked
package io.github.ilumar589.jecs.entity;

import org.jspecify.annotations.NullMarked;
