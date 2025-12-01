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
 * // Define a physics system
 * System physics = new System.Builder("Physics")
 *     .withMutable(Position.class)
 *     .withReadOnly(Velocity.class)
 *     .execute((world, query) -> {
 *         query.forEachWithAccess(Position.class, Velocity.class, (pos, vel) -> {
 *             pos.update(p -> new Position(
 *                 p.x() + vel.get().dx(),
 *                 p.y() + vel.get().dy(),
 *                 p.z() + vel.get().dz()
 *             ));
 *         });
 *     })
 *     .build();
 *
 * // Create scheduler and execute
 * SystemScheduler scheduler = new SystemScheduler.Builder()
 *     .addSystem(inputSystem)
 *     .addSystem(physics)
 *     .addSystem(renderSystem)
 *     .runAfter(renderSystem, physics)
 *     .build();
 *
 * scheduler.execute(world);
 * }</pre>
 *
 * <h2>Parallelization</h2>
 * Two systems can run in parallel if:
 * <ul>
 *   <li>They don't write to the same component types</li>
 *   <li>One doesn't write to a component the other reads</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * Systems in the same stage run in parallel using a configurable ExecutorService.
 * The default thread pool size is Runtime.availableProcessors().
 *
 * <h2>Null Safety</h2>
 * This package uses JSpecify annotations for null safety.
 * All types are non-null by default unless annotated with {@code @Nullable}.
 */
@NullMarked
package io.github.ilumar589.jecs.system;

import org.jspecify.annotations.NullMarked;
