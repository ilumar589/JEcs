package io.github.ilumar589.jecs.system;

import io.github.ilumar589.jecs.world.EcsWorld;

import java.util.*;
import java.util.concurrent.*;

/**
 * Schedules and executes systems according to their component access patterns and modes.
 * Automatically determines which systems can run in parallel.
 *
 * <h2>System Modes</h2>
 * The scheduler supports three execution modes via {@link SystemMode}:
 * <ul>
 *   <li>{@link SystemMode#STARTUP} - Systems that run once during initialization</li>
 *   <li>{@link SystemMode#UPDATE} - Systems that run repeatedly in the game loop (default)</li>
 *   <li>{@link SystemMode#SHUTDOWN} - Systems that run once during cleanup</li>
 * </ul>
 *
 * <h2>Game Loop Integration</h2>
 * <pre>{@code
 * // Define startup systems
 * System resourceLoader = new System.Builder("ResourceLoader")
 *     .mode(SystemMode.STARTUP)
 *     .execute((w, q) -> {
 *         // load resources
 *     })
 *     .build();
 *
 * // Define update systems (default mode)
 * System physics = new System.Builder("Physics")
 *     .withMutable(Position.class)
 *     .withReadOnly(Velocity.class)
 *     .execute((w, q) -> {
 *         // physics update
 *     })
 *     .build();
 *
 * // Build scheduler
 * SystemScheduler scheduler = new SystemScheduler.Builder()
 *     .addSystems(resourceLoader, physics)
 *     .build();
 *
 * // Game lifecycle
 * scheduler.executeStartup(world);    // Run once at init
 * while (running) {
 *     scheduler.executeUpdate(world);  // Run every frame
 * }
 * scheduler.executeShutdown(world);   // Run once at cleanup
 * }</pre>
 *
 * <h2>Automatic Parallelization</h2>
 * The scheduler automatically groups systems into parallel stages based on their
 * component access patterns. Systems that do not conflict (do not read/write the same
 * components) will run in parallel automatically.
 *
 * <h2>Conflict Rules</h2>
 * Two systems conflict and must run sequentially if:
 * <ul>
 *   <li>Both write to the same component type</li>
 *   <li>One writes to a component that the other reads</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Physics writes Position, AI writes Velocity, Audio has no conflicts
 * // The scheduler automatically determines: Physics and AI can't run together
 * // (if Physics reads Velocity), but Audio can run with either.
 * SystemScheduler scheduler = new SystemScheduler.Builder()
 *     .addSystem(physicsSystem)   // writes Position, reads Velocity
 *     .addSystem(aiSystem)        // writes Velocity
 *     .addSystem(audioSystem)     // reads AudioSource only
 *     .addSystem(renderSystem)    // reads Position
 *     .build();
 *
 * // The scheduler automatically computes stages:
 * // Stage 1: [aiSystem, audioSystem] - can run in parallel
 * // Stage 2: [physicsSystem] - must wait for aiSystem (reads Velocity)
 * // Stage 3: [renderSystem] - must wait for physicsSystem (reads Position)
 *
 * scheduler.execute(world);
 * }</pre>
 *
 * <h2>Explicit Ordering (Optional)</h2>
 * Use {@code runInSequence()} only when you need to enforce a specific order
 * beyond what the automatic conflict detection provides:
 * <pre>{@code
 * scheduler.runInSequence(inputSystem, physicsSystem);  // Input must run before Physics
 * }</pre>
 *
 * <h2>Virtual Threads</h2>
 * By default, the scheduler uses virtual threads (Project Loom) for lightweight
 * concurrent execution. Virtual threads are ideal for ECS systems as they have
 * minimal overhead and can scale to many concurrent tasks efficiently.
 *
 * <h2>Thread Safety</h2>
 * Systems in the same stage run concurrently using virtual threads.
 * A custom ExecutorService can be provided if needed.
 */
public final class SystemScheduler {

    private final List<SystemStage> stages;
    private final List<SystemStage> startupStages;
    private final List<SystemStage> updateStages;
    private final List<SystemStage> shutdownStages;
    private final ExecutorService executor;
    private final boolean parallel;

    private SystemScheduler(List<SystemStage> stages, 
                           List<SystemStage> startupStages,
                           List<SystemStage> updateStages,
                           List<SystemStage> shutdownStages,
                           ExecutorService executor, 
                           boolean parallel) {
        this.stages = Collections.unmodifiableList(new ArrayList<>(stages));
        this.startupStages = Collections.unmodifiableList(new ArrayList<>(startupStages));
        this.updateStages = Collections.unmodifiableList(new ArrayList<>(updateStages));
        this.shutdownStages = Collections.unmodifiableList(new ArrayList<>(shutdownStages));
        this.executor = executor;
        this.parallel = parallel;
    }

    /**
     * Executes all systems in order, respecting stage boundaries.
     * Systems within a stage may run in parallel if parallel mode is enabled.
     *
     * <p>This method executes all systems regardless of their mode. For mode-specific
     * execution in a game loop, prefer using {@link #executeStartup(EcsWorld)},
     * {@link #executeUpdate(EcsWorld)}, and {@link #executeShutdown(EcsWorld)}.</p>
     *
     * @param world the ECS world to operate on
     */
    public void execute(EcsWorld world) {
        executeStages(stages, world);
    }

    /**
     * Executes only systems with {@link SystemMode#STARTUP} mode.
     * These systems are intended to run once during initialization.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * // In game initialization
     * scheduler.executeStartup(world);
     *
     * // In game loop
     * while (running) {
     *     scheduler.executeUpdate(world);
     * }
     * }</pre>
     *
     * @param world the ECS world to operate on
     */
    public void executeStartup(EcsWorld world) {
        executeStages(startupStages, world);
    }

    /**
     * Executes only systems with {@link SystemMode#UPDATE} mode.
     * These systems are intended to run repeatedly in the game loop.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * // In game loop
     * while (running) {
     *     scheduler.executeUpdate(world);
     * }
     * }</pre>
     *
     * @param world the ECS world to operate on
     */
    public void executeUpdate(EcsWorld world) {
        executeStages(updateStages, world);
    }

    /**
     * Executes only systems with {@link SystemMode#SHUTDOWN} mode.
     * These systems are intended to run once during cleanup.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * // On game exit
     * scheduler.executeShutdown(world);
     * scheduler.shutdown();
     * }</pre>
     *
     * @param world the ECS world to operate on
     */
    public void executeShutdown(EcsWorld world) {
        executeStages(shutdownStages, world);
    }

    private void executeStages(List<SystemStage> stagesToExecute, EcsWorld world) {
        for (SystemStage stage : stagesToExecute) {
            if (parallel && stage.size() > 1) {
                executeParallel(stage, world);
            } else {
                executeSequential(stage, world);
            }
        }
    }

    private void executeSequential(SystemStage stage, EcsWorld world) {
        for (System system : stage.getSystems()) {
            system.execute(world);
        }
    }

    private void executeParallel(SystemStage stage, EcsWorld world) {
        List<Future<?>> futures = new ArrayList<>();
        for (System system : stage.getSystems()) {
            futures.add(executor.submit(() -> system.execute(world)));
        }

        // Wait for all systems in stage to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("System execution interrupted", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("System execution failed", e.getCause());
            }
        }
    }

    /**
     * Returns the computed stages for all systems.
     *
     * @return unmodifiable list of stages
     */
    public List<SystemStage> getStages() {
        return stages;
    }

    /**
     * Returns the computed stages for startup systems only.
     *
     * @return unmodifiable list of startup stages
     */
    public List<SystemStage> getStartupStages() {
        return startupStages;
    }

    /**
     * Returns the computed stages for update systems only.
     *
     * @return unmodifiable list of update stages
     */
    public List<SystemStage> getUpdateStages() {
        return updateStages;
    }

    /**
     * Returns the computed stages for shutdown systems only.
     *
     * @return unmodifiable list of shutdown stages
     */
    public List<SystemStage> getShutdownStages() {
        return shutdownStages;
    }

    /**
     * Returns whether parallel execution is enabled.
     *
     * @return true if parallel mode is enabled
     */
    public boolean isParallel() {
        return parallel;
    }

    /**
     * Shuts down the executor service.
     * Call this when the scheduler is no longer needed.
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Shuts down the executor service and waits for termination.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit
     * @return true if the executor terminated before the timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean shutdownAndAwait(long timeout, TimeUnit unit) throws InterruptedException {
        executor.shutdown();
        return executor.awaitTermination(timeout, unit);
    }

    /**
     * Builder for creating SystemScheduler instances.
     */
    public static final class Builder {
        private final List<System> systems = new ArrayList<>();
        private final Map<System, Set<System>> dependencies = new HashMap<>();
        private ExecutorService executor;
        private boolean parallel = true;

        /**
         * Adds a system to be scheduled.
         *
         * @param system the system to add
         * @return this builder for chaining
         */
        public Builder addSystem(System system) {
            systems.add(system);
            return this;
        }

        /**
         * Adds multiple systems to be scheduled.
         *
         * @param systems the systems to add
         * @return this builder for chaining
         */
        public Builder addSystems(System... systems) {
            for (System system : systems) {
                this.systems.add(system);
            }
            return this;
        }

        /**
         * Specifies that the given systems must run in the specified order.
         * Use this only when you need to enforce ordering beyond what the
         * automatic conflict detection provides.
         *
         * <p>Most of the time, you don't need this - the scheduler automatically
         * determines the correct order based on component access patterns.</p>
         *
         * <h2>Usage Example</h2>
         * <pre>{@code
         * // Force Input to run before Physics even if they don't have
         * // conflicting component access
         * scheduler.runInSequence(inputSystem, physicsSystem);
         * }</pre>
         *
         * @param systems the systems in the order they should execute
         * @return this builder for chaining
         */
        public Builder runInSequence(System... systems) {
            for (int i = 1; i < systems.length; i++) {
                dependencies.computeIfAbsent(systems[i], k -> new HashSet<>()).add(systems[i - 1]);
            }
            return this;
        }

        /**
         * Sets a custom executor service for parallel execution.
         * If not set, a virtual thread per task executor is used (recommended).
         *
         * @param executor the executor service
         * @return this builder for chaining
         */
        public Builder withExecutor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Enables or disables parallel execution.
         * Default is enabled.
         *
         * @param parallel true to enable parallel execution
         * @return this builder for chaining
         */
        public Builder parallel(boolean parallel) {
            this.parallel = parallel;
            return this;
        }

        /**
         * Builds the SystemScheduler.
         * Computes stages based on dependencies and component access conflicts.
         * Each system mode (STARTUP, UPDATE, SHUTDOWN) has its own independent stages.
         *
         * @return the built SystemScheduler
         * @throws IllegalStateException if circular dependencies are detected
         */
        public SystemScheduler build() {
            if (executor == null) {
                // Use virtual threads for lightweight concurrent execution
                executor = Executors.newVirtualThreadPerTaskExecutor();
            }

            // Check for circular dependencies
            detectCircularDependencies();

            // Compute stages for all systems (backward compatibility)
            List<SystemStage> stages = computeStages(systems);

            // Filter systems by mode and compute separate stages
            List<System> startupSystems = filterByMode(SystemMode.STARTUP);
            List<System> updateSystems = filterByMode(SystemMode.UPDATE);
            List<System> shutdownSystems = filterByMode(SystemMode.SHUTDOWN);

            List<SystemStage> startupStages = computeStages(startupSystems);
            List<SystemStage> updateStages = computeStages(updateSystems);
            List<SystemStage> shutdownStages = computeStages(shutdownSystems);

            return new SystemScheduler(stages, startupStages, updateStages, shutdownStages, executor, parallel);
        }

        private List<System> filterByMode(SystemMode mode) {
            List<System> filtered = new ArrayList<>();
            for (System system : systems) {
                if (system.getMode() == mode) {
                    filtered.add(system);
                }
            }
            return filtered;
        }

        private void detectCircularDependencies() {
            Set<System> visited = new HashSet<>();
            Set<System> recursionStack = new HashSet<>();

            for (System system : systems) {
                if (hasCycle(system, visited, recursionStack)) {
                    throw new IllegalStateException("Circular dependency detected involving system: " + system.getName());
                }
            }
        }

        private boolean hasCycle(System system, Set<System> visited, Set<System> recursionStack) {
            if (recursionStack.contains(system)) {
                return true;
            }
            if (visited.contains(system)) {
                return false;
            }

            visited.add(system);
            recursionStack.add(system);

            Set<System> deps = dependencies.getOrDefault(system, Collections.emptySet());
            for (System dep : deps) {
                if (hasCycle(dep, visited, recursionStack)) {
                    return true;
                }
            }

            recursionStack.remove(system);
            return false;
        }

        private List<SystemStage> computeStages(List<System> systemsToSchedule) {
            if (systemsToSchedule.isEmpty()) {
                return Collections.emptyList();
            }

            // Create a set for quick lookup
            Set<System> systemSet = new HashSet<>(systemsToSchedule);

            // Build complete dependency graph including access conflicts
            Map<System, Set<System>> fullDeps = new HashMap<>();
            for (System system : systemsToSchedule) {
                // Only include dependencies that are in the same system set
                Set<System> deps = new HashSet<>();
                for (System dep : dependencies.getOrDefault(system, Collections.emptySet())) {
                    if (systemSet.contains(dep)) {
                        deps.add(dep);
                    }
                }
                fullDeps.put(system, deps);
            }

            // Add implicit dependencies from access conflicts
            // A system must wait for any earlier conflicting system
            for (int i = 0; i < systemsToSchedule.size(); i++) {
                System current = systemsToSchedule.get(i);
                for (int j = 0; j < i; j++) {
                    System earlier = systemsToSchedule.get(j);
                    if (current.conflictsWith(earlier)) {
                        // Check if there isn't already a dependency path
                        fullDeps.computeIfAbsent(current, k -> new HashSet<>()).add(earlier);
                    }
                }
            }

            // Topological sort with stage assignment
            List<SystemStage> stages = new ArrayList<>();
            Set<System> scheduled = new HashSet<>();
            Set<System> remaining = new HashSet<>(systemsToSchedule);

            while (!remaining.isEmpty()) {
                // Find all systems whose dependencies are satisfied
                List<System> ready = new ArrayList<>();
                for (System system : remaining) {
                    Set<System> deps = fullDeps.getOrDefault(system, Collections.emptySet());
                    if (scheduled.containsAll(deps)) {
                        ready.add(system);
                    }
                }

                if (ready.isEmpty()) {
                    throw new IllegalStateException("Unable to schedule remaining systems - possible circular dependency");
                }

                // Group non-conflicting ready systems into the same stage
                List<System> stageSystem = new ArrayList<>();

                for (System system : ready) {
                    boolean canAdd = true;
                    for (System inStage : stageSystem) {
                        if (system.conflictsWith(inStage)) {
                            canAdd = false;
                            break;
                        }
                    }
                    if (canAdd) {
                        stageSystem.add(system);
                    }
                    // Systems that can't be added stay in 'remaining' for the next iteration
                }

                stages.add(new SystemStage(stageSystem));
                scheduled.addAll(stageSystem);
                remaining.removeAll(stageSystem);
            }

            return stages;
        }
    }
}
