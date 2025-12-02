package io.github.ilumar589.jecs.system;

/**
 * Defines when a system should be executed in the application lifecycle.
 * This enum enables distinguishing between systems that run once at startup,
 * systems that run repeatedly in the update loop, and systems that run once
 * during cleanup, similar to Bevy ECS.
 *
 * <h2>Usage in Game Loop</h2>
 * <pre>{@code
 * // Define startup systems (run once at initialization)
 * System resourceLoader = new System.Builder("ResourceLoader")
 *     .mode(SystemMode.STARTUP)
 *     .execute((w, q) -> {
 *         // Load textures, sounds, etc.
 *     })
 *     .build();
 *
 * // Define update systems (run every frame)
 * System physics = new System.Builder("Physics")
 *     .mode(SystemMode.UPDATE)  // or omit for default
 *     .withMutable(Position.class)
 *     .withReadOnly(Velocity.class)
 *     .execute((w, q) -> {
 *         // Update physics each frame
 *     })
 *     .build();
 *
 * // Define shutdown systems (run once at cleanup)
 * System saveGame = new System.Builder("SaveGame")
 *     .mode(SystemMode.SHUTDOWN)
 *     .execute((w, q) -> {
 *         // Save game state
 *     })
 *     .build();
 *
 * // Build and use the scheduler
 * SystemScheduler scheduler = new SystemScheduler.Builder()
 *     .addSystems(resourceLoader, physics, saveGame)
 *     .build();
 *
 * // Lifecycle execution
 * scheduler.executeStartup(world);    // Run startup systems once
 * while (running) {
 *     scheduler.executeUpdate(world);  // Run update systems each frame
 * }
 * scheduler.executeShutdown(world);   // Run shutdown systems once
 * }</pre>
 *
 * <h2>LibGDX Integration Example</h2>
 * <pre>{@code
 * public class MyGame extends ApplicationAdapter {
 *     private EcsWorld world;
 *     private SystemScheduler scheduler;
 *
 *     public void create() {
 *         world = new EcsWorld();
 *         scheduler = buildScheduler();
 *         scheduler.executeStartup(world);  // Initialize resources
 *     }
 *
 *     public void render() {
 *         scheduler.executeUpdate(world);   // Game loop
 *     }
 *
 *     public void dispose() {
 *         scheduler.executeShutdown(world); // Cleanup
 *         scheduler.shutdown();
 *     }
 * }
 * }</pre>
 *
 * @see System.Builder#mode(SystemMode)
 * @see SystemScheduler#executeStartup(io.github.ilumar589.jecs.world.EcsWorld)
 * @see SystemScheduler#executeUpdate(io.github.ilumar589.jecs.world.EcsWorld)
 * @see SystemScheduler#executeShutdown(io.github.ilumar589.jecs.world.EcsWorld)
 */
public enum SystemMode {

    /**
     * Systems that run once during initialization.
     * Use for resource loading, world initialization, configuration, etc.
     *
     * <h2>Example Use Cases</h2>
     * <ul>
     *   <li>Loading textures, sounds, and other assets</li>
     *   <li>Spawning initial entities</li>
     *   <li>Initializing game configuration</li>
     *   <li>Setting up audio subsystem</li>
     *   <li>Configuring input handlers</li>
     * </ul>
     */
    STARTUP,

    /**
     * Systems that run repeatedly in the game loop.
     * This is the default mode for systems if not specified.
     *
     * <h2>Example Use Cases</h2>
     * <ul>
     *   <li>Physics simulation</li>
     *   <li>Rendering</li>
     *   <li>AI decision making</li>
     *   <li>Input processing</li>
     *   <li>Animation updates</li>
     *   <li>Collision detection</li>
     * </ul>
     */
    UPDATE,

    /**
     * Systems that run once during cleanup.
     * Use for saving state, releasing resources, etc.
     *
     * <h2>Example Use Cases</h2>
     * <ul>
     *   <li>Saving game state</li>
     *   <li>Releasing resources</li>
     *   <li>Cleanup operations</li>
     *   <li>Logging final statistics</li>
     * </ul>
     */
    SHUTDOWN
}
