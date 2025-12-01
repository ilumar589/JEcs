/**
 * Systems API for the JEcs ECS framework.
 *
 * <h2>Overview</h2>
 * This package provides a comprehensive Systems API for defining and scheduling
 * ECS systems with automatic parallelization based on component access patterns.
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link io.github.ilumar589.jecs.system.System} - Core system class with builder pattern</li>
 *   <li>{@link io.github.ilumar589.jecs.system.ComponentAccess} - Describes read/write/exclude requirements</li>
 *   <li>{@link io.github.ilumar589.jecs.system.SystemScheduler} - Schedules and executes systems</li>
 *   <li>{@link io.github.ilumar589.jecs.system.SystemStage} - Represents a group of parallel-safe systems</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Define systems with their component access patterns
 * System physics = new System.Builder("Physics")
 *     .withMutable(Position.class)
 *     .withReadOnly(Velocity.class)
 *     .execute((world, query) -> {
 *         query.forEachWithAccess(Position.class, Velocity.class, (pos, vel) -> {
 *             Position current = pos.get();
 *             Velocity velocity = vel.get();
 *             pos.set(new Position(
 *                 current.x() + velocity.dx(),
 *                 current.y() + velocity.dy(),
 *                 current.z() + velocity.dz()
 *             ));
 *         });
 *     })
 *     .build();
 *
 * // The scheduler automatically determines which systems can run in parallel
 * SystemScheduler scheduler = new SystemScheduler.Builder()
 *     .addSystem(physics)
 *     .addSystem(aiSystem)
 *     .addSystem(renderSystem)
 *     .build();
 *
 * scheduler.execute(world);
 * }</pre>
 *
 * <h2>Automatic Parallelization</h2>
 * The scheduler automatically determines which systems can run in parallel based on
 * their component access patterns. Two systems can run in parallel if:
 * <ul>
 *   <li>They don't write to the same component types</li>
 *   <li>One doesn't write to a component the other reads</li>
 * </ul>
 *
 * <h2>Virtual Threads</h2>
 * The scheduler uses virtual threads (Project Loom) by default for lightweight
 * concurrent execution with minimal overhead.
 *
 * <h2>Null Safety</h2>
 * This package uses JSpecify annotations for null safety.
 * All types are non-null by default unless annotated with {@code @Nullable}.
 */
@NullMarked
package io.github.ilumar589.jecs.system;

import org.jspecify.annotations.NullMarked;
