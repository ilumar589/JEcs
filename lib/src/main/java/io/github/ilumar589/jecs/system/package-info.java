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
 *   <li>{@link io.github.ilumar589.jecs.system.SystemMode} - Enum defining when systems run (STARTUP, UPDATE, SHUTDOWN)</li>
 *   <li>{@link io.github.ilumar589.jecs.system.ComponentAccess} - Describes read/write/exclude requirements</li>
 *   <li>{@link io.github.ilumar589.jecs.system.SystemScheduler} - Schedules and executes systems</li>
 *   <li>{@link io.github.ilumar589.jecs.system.SystemStage} - Represents a group of parallel-safe systems</li>
 * </ul>
 *
 * <h2>System Modes</h2>
 * Systems can be configured to run at different lifecycle stages:
 * <ul>
 *   <li>{@link io.github.ilumar589.jecs.system.SystemMode#STARTUP} - Run once during initialization</li>
 *   <li>{@link io.github.ilumar589.jecs.system.SystemMode#UPDATE} - Run repeatedly in game loop (default)</li>
 *   <li>{@link io.github.ilumar589.jecs.system.SystemMode#SHUTDOWN} - Run once during cleanup</li>
 * </ul>
 *
 * <h2>Complete Usage Example</h2>
 * <pre>{@code
 * // Define startup systems (run once at initialization)
 * System resourceLoader = new System.Builder("ResourceLoader")
 *     .mode(SystemMode.STARTUP)
 *     .execute((world, query) -> {
 *         // Load textures, sounds, etc.
 *     })
 *     .build();
 *
 * System worldInitializer = new System.Builder("WorldInitializer")
 *     .mode(SystemMode.STARTUP)
 *     .withMutable(Position.class, Health.class)
 *     .execute((world, query) -> {
 *         // Spawn initial entities
 *         world.spawn(new Position(0, 0, 0), new Health(100, 100));
 *     })
 *     .build();
 *
 * // Define update systems (run every frame)
 * System physics = new System.Builder("Physics")
 *     .mode(SystemMode.UPDATE)  // Optional, UPDATE is default
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
 * System render = new System.Builder("Render")
 *     .withReadOnly(Position.class)
 *     .execute((world, query) -> {
 *         // Render entities
 *     })
 *     .build();
 *
 * // Define shutdown systems (run once at cleanup)
 * System saveGame = new System.Builder("SaveGame")
 *     .mode(SystemMode.SHUTDOWN)
 *     .execute((world, query) -> {
 *         // Save game state
 *     })
 *     .build();
 *
 * // Build the scheduler with all systems
 * SystemScheduler scheduler = new SystemScheduler.Builder()
 *     .addSystems(resourceLoader, worldInitializer, physics, render, saveGame)
 *     .build();
 *
 * // Game initialization
 * scheduler.executeStartup(world);
 *
 * // Game loop
 * while (running) {
 *     scheduler.executeUpdate(world);
 * }
 *
 * // Game cleanup
 * scheduler.executeShutdown(world);
 * scheduler.shutdown();
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
