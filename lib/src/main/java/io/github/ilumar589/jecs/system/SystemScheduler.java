package io.github.ilumar589.jecs.system;

import io.github.ilumar589.jecs.world.EcsWorld;

import java.util.*;
import java.util.concurrent.*;

/**
 * Schedules and executes systems according to their dependencies and access patterns.
 * Automatically computes execution stages for parallelization.
 *
 * <h2>Automatic Parallelization</h2>
 * Systems are automatically grouped into stages based on:
 * <ul>
 *   <li>Explicit dependencies via {@code runAfter()}</li>
 *   <li>Implicit conflicts from component access patterns</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * SystemScheduler scheduler = new SystemScheduler.Builder()
 *     .addSystem(inputSystem)
 *     .addSystem(physicsSystem)
 *     .addSystem(renderSystem)
 *     .runAfter(renderSystem, physicsSystem)  // Explicit dependency
 *     .build();
 *
 * // Execute all systems
 * scheduler.execute(world);
 *
 * // Shutdown when done
 * scheduler.shutdown();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * The scheduler uses a configurable ExecutorService for parallel execution.
 * Systems in the same stage run concurrently if parallel mode is enabled.
 */
public final class SystemScheduler {

    private final List<SystemStage> stages;
    private final ExecutorService executor;
    private final boolean parallel;

    private SystemScheduler(List<SystemStage> stages, ExecutorService executor, boolean parallel) {
        this.stages = Collections.unmodifiableList(new ArrayList<>(stages));
        this.executor = executor;
        this.parallel = parallel;
    }

    /**
     * Executes all systems in order, respecting stage boundaries.
     * Systems within a stage may run in parallel if parallel mode is enabled.
     *
     * @param world the ECS world to operate on
     */
    public void execute(EcsWorld world) {
        for (SystemStage stage : stages) {
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
     * Returns the computed stages.
     *
     * @return unmodifiable list of stages
     */
    public List<SystemStage> getStages() {
        return stages;
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
         * Specifies that one system must run after another.
         *
         * @param system the system that runs later
         * @param dependency the system that must run first
         * @return this builder for chaining
         */
        public Builder runAfter(System system, System dependency) {
            dependencies.computeIfAbsent(system, k -> new HashSet<>()).add(dependency);
            return this;
        }

        /**
         * Sets a custom executor service for parallel execution.
         * If not set, a fixed thread pool based on available processors is used.
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
         *
         * @return the built SystemScheduler
         * @throws IllegalStateException if circular dependencies are detected
         */
        public SystemScheduler build() {
            if (executor == null) {
                executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            }

            // Check for circular dependencies
            detectCircularDependencies();

            // Compute stages
            List<SystemStage> stages = computeStages();

            return new SystemScheduler(stages, executor, parallel);
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

        private List<SystemStage> computeStages() {
            // Build complete dependency graph including access conflicts
            Map<System, Set<System>> fullDeps = new HashMap<>();
            for (System system : systems) {
                Set<System> deps = new HashSet<>(dependencies.getOrDefault(system, Collections.emptySet()));
                fullDeps.put(system, deps);
            }

            // Add implicit dependencies from access conflicts
            // A system must wait for any earlier conflicting system
            for (int i = 0; i < systems.size(); i++) {
                System current = systems.get(i);
                for (int j = 0; j < i; j++) {
                    System earlier = systems.get(j);
                    if (current.conflictsWith(earlier)) {
                        // Check if there isn't already a dependency path
                        fullDeps.computeIfAbsent(current, k -> new HashSet<>()).add(earlier);
                    }
                }
            }

            // Topological sort with stage assignment
            List<SystemStage> stages = new ArrayList<>();
            Set<System> scheduled = new HashSet<>();
            Set<System> remaining = new HashSet<>(systems);

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
                List<System> deferred = new ArrayList<>();

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
                    } else {
                        deferred.add(system);
                    }
                }

                stages.add(new SystemStage(stageSystem));
                scheduled.addAll(stageSystem);
                remaining.removeAll(stageSystem);
            }

            return stages;
        }
    }
}
