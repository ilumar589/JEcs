package io.github.ilumar589.jecs.benchmark;

import io.github.ilumar589.jecs.component.Health;
import io.github.ilumar589.jecs.component.Position;
import io.github.ilumar589.jecs.component.Velocity;
import io.github.ilumar589.jecs.system.System;
import io.github.ilumar589.jecs.system.SystemMode;
import io.github.ilumar589.jecs.system.SystemScheduler;
import io.github.ilumar589.jecs.world.EcsWorld;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for large-scale system execution with SystemMode.
 * Tests performance of startup vs update systems, parallel execution efficiency,
 * and stage computation for large system counts.
 *
 * <h2>Benchmark Scenarios</h2>
 * <ul>
 *   <li>200 total systems split between startup (50) and update (150)</li>
 *   <li>Component access patterns: read-only, write, and conflicting systems</li>
 *   <li>Entity counts: 1000, 10000, 100000</li>
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class LargeScaleSystemBenchmark {

    @Param({"1000", "10000", "100000"})
    private int entityCount;

    private EcsWorld world;
    private SystemScheduler schedulerWithModes;
    private SystemScheduler sequentialScheduler;
    private SystemScheduler parallelScheduler;

    // Systems categorized by type
    private List<System> startupSystems;
    private List<System> updateSystems;

    // Volatile sink to prevent dead code elimination in benchmark lambdas
    private volatile double sink;

    @Setup(Level.Trial)
    public void setup() {
        world = new EcsWorld();

        // Create entities with various component combinations
        for (int i = 0; i < entityCount; i++) {
            if (i % 3 == 0) {
                world.spawn(
                        new Position(i, 0, 0),
                        new Velocity(1, 0, 0),
                        new Health(100, 100)
                );
            } else if (i % 3 == 1) {
                world.spawn(
                        new Position(i, 0, 0),
                        new Velocity(1, 1, 0)
                );
            } else {
                world.spawn(
                        new Position(i, 0, 0),
                        new Health(50, 100)
                );
            }
        }

        startupSystems = createStartupSystems(50);
        updateSystems = createUpdateSystems(150);

        // Build scheduler with all systems
        SystemScheduler.Builder builder = new SystemScheduler.Builder();
        startupSystems.forEach(builder::addSystem);
        updateSystems.forEach(builder::addSystem);
        schedulerWithModes = builder.build();

        // Build parallel scheduler with only update systems
        SystemScheduler.Builder parallelBuilder = new SystemScheduler.Builder()
                .parallel(true);
        updateSystems.forEach(parallelBuilder::addSystem);
        parallelScheduler = parallelBuilder.build();

        // Build sequential scheduler with only update systems
        SystemScheduler.Builder sequentialBuilder = new SystemScheduler.Builder()
                .parallel(false);
        updateSystems.forEach(sequentialBuilder::addSystem);
        sequentialScheduler = sequentialBuilder.build();
    }

    /**
     * Creates 50 startup systems with various patterns:
     * - Resource loaders (read-only Position)
     * - World initializers (mutable Health)
     * - Config loaders (no components)
     * - Audio setup (no components)
     * - Input setup (read-only Velocity)
     */
    private List<System> createStartupSystems(int count) {
        List<System> systems = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int category = i % 5;
            System system;

            switch (category) {
                case 0 -> {
                    // ResourceLoader - read Position
                    system = new System.Builder("ResourceLoader_" + i)
                            .mode(SystemMode.STARTUP)
                            .withReadOnly(Position.class)
                            .execute((w, q) -> {
                                q.forEach(Position.class, pos -> {
                                    // Simulate resource loading
                                    sink = pos.x() + pos.y();
                                });
                            })
                            .build();
                }
                case 1 -> {
                    // WorldInitializer - mutable Health
                    system = new System.Builder("WorldInitializer_" + i)
                            .mode(SystemMode.STARTUP)
                            .withMutable(Health.class)
                            .execute((w, q) -> {
                                q.forEachMutable(Health.class, h -> {
                                    // Initialize health
                                    h.set(new Health(100, 100));
                                });
                            })
                            .build();
                }
                case 2 -> {
                    // ConfigLoader - no components
                    system = new System.Builder("ConfigLoader_" + i)
                            .mode(SystemMode.STARTUP)
                            .execute((w, q) -> {
                                // Simulate config loading
                                sink = 1 + 2;
                            })
                            .build();
                }
                case 3 -> {
                    // AudioSetup - no components
                    system = new System.Builder("AudioSetup_" + i)
                            .mode(SystemMode.STARTUP)
                            .execute((w, q) -> {
                                // Simulate audio setup
                                sink = 0.8;
                            })
                            .build();
                }
                default -> {
                    // InputSetup - read Velocity
                    system = new System.Builder("InputSetup_" + i)
                            .mode(SystemMode.STARTUP)
                            .withReadOnly(Velocity.class)
                            .execute((w, q) -> {
                                q.forEach(Velocity.class, vel -> {
                                    // Simulate input configuration
                                    sink = vel.dx();
                                });
                            })
                            .build();
                }
            }
            systems.add(system);
        }

        return systems;
    }

    /**
     * Creates 150 update systems with various patterns:
     * - 50 read-only systems (can parallelize)
     * - 50 systems writing to different components (can parallelize)
     * - 50 systems with conflicts (must serialize)
     */
    private List<System> createUpdateSystems(int count) {
        List<System> systems = new ArrayList<>();

        // 50 read-only systems (can all run in parallel)
        for (int i = 0; i < 50; i++) {
            systems.add(new System.Builder("Reader_" + i)
                    .mode(SystemMode.UPDATE)
                    .withReadOnly(Position.class)
                    .execute((w, q) -> {
                        q.forEach(Position.class, pos -> {
                            sink = pos.x() + pos.y() + pos.z();
                        });
                    })
                    .build());
        }

        // 50 systems writing to different component combinations
        for (int i = 0; i < 50; i++) {
            int variant = i % 3;
            System system;

            switch (variant) {
                case 0 -> {
                    // Physics - writes Position
                    system = new System.Builder("Physics_" + i)
                            .mode(SystemMode.UPDATE)
                            .withMutable(Position.class)
                            .withReadOnly(Velocity.class)
                            .execute((w, q) -> {
                                q.forEachWithAccess(Position.class, Velocity.class, (pos, vel) -> {
                                    pos.set(new Position(
                                            pos.get().x() + vel.get().dx(),
                                            pos.get().y() + vel.get().dy(),
                                            pos.get().z() + vel.get().dz()
                                    ));
                                });
                            })
                            .build();
                }
                case 1 -> {
                    // AI - writes Velocity
                    system = new System.Builder("AI_" + i)
                            .mode(SystemMode.UPDATE)
                            .withMutable(Velocity.class)
                            .execute((w, q) -> {
                                q.forEachMutable(Velocity.class, vel -> {
                                    // Simulate AI decision
                                    vel.set(new Velocity(
                                            vel.get().dx() * 0.99f,
                                            vel.get().dy() * 0.99f,
                                            vel.get().dz() * 0.99f
                                    ));
                                });
                            })
                            .build();
                }
                default -> {
                    // Health - writes Health
                    system = new System.Builder("HealthSystem_" + i)
                            .mode(SystemMode.UPDATE)
                            .withMutable(Health.class)
                            .execute((w, q) -> {
                                q.forEachMutable(Health.class, h -> {
                                    if (h.get().current() < h.get().max()) {
                                        h.set(new Health(h.get().current() + 1, h.get().max()));
                                    }
                                });
                            })
                            .build();
                }
            }
            systems.add(system);
        }

        // 50 systems with conflicts (read what others write)
        for (int i = 0; i < 50; i++) {
            int variant = i % 3;
            System system;

            switch (variant) {
                case 0 -> {
                    // Render - reads Position (conflicts with Physics)
                    system = new System.Builder("Render_" + i)
                            .mode(SystemMode.UPDATE)
                            .withReadOnly(Position.class)
                            .execute((w, q) -> {
                                q.forEach(Position.class, pos -> {
                                    // Simulate rendering
                                    sink = pos.x() * 10 + pos.y() * 10;
                                });
                            })
                            .build();
                }
                case 1 -> {
                    // Animation - reads Velocity (conflicts with AI)
                    system = new System.Builder("Animation_" + i)
                            .mode(SystemMode.UPDATE)
                            .withReadOnly(Velocity.class)
                            .execute((w, q) -> {
                                q.forEach(Velocity.class, vel -> {
                                    // Simulate animation speed calculation
                                    sink = Math.sqrt(
                                            vel.dx() * vel.dx() +
                                                    vel.dy() * vel.dy() +
                                                    vel.dz() * vel.dz()
                                    );
                                });
                            })
                            .build();
                }
                default -> {
                    // UI - reads Health (conflicts with HealthSystem)
                    system = new System.Builder("UI_" + i)
                            .mode(SystemMode.UPDATE)
                            .withReadOnly(Health.class)
                            .execute((w, q) -> {
                                q.forEach(Health.class, health -> {
                                    // Simulate UI update
                                    sink = (double) health.current() / health.max() * 100;
                                });
                            })
                            .build();
                }
            }
            systems.add(system);
        }

        return systems;
    }

    @TearDown(Level.Trial)
    public void tearDown() throws InterruptedException {
        schedulerWithModes.shutdown();
        sequentialScheduler.shutdown();
        parallelScheduler.shutdown();
    }

    /**
     * Benchmark: Execute 50 startup systems once.
     */
    @Benchmark
    public void startup50Systems(Blackhole bh) {
        schedulerWithModes.executeStartup(world);
        bh.consume(world);
    }

    /**
     * Benchmark: Execute 150 update systems in one frame.
     */
    @Benchmark
    public void update150Systems(Blackhole bh) {
        schedulerWithModes.executeUpdate(world);
        bh.consume(world);
    }

    /**
     * Benchmark: Execute full startup then 10 update frames.
     */
    @Benchmark
    public void mixedExecution(Blackhole bh) {
        schedulerWithModes.executeStartup(world);
        for (int i = 0; i < 10; i++) {
            schedulerWithModes.executeUpdate(world);
        }
        bh.consume(world);
    }

    /**
     * Benchmark: Compare sequential execution of 150 update systems.
     */
    @Benchmark
    public void update150SystemsSequential(Blackhole bh) {
        sequentialScheduler.execute(world);
        bh.consume(world);
    }

    /**
     * Benchmark: Compare parallel execution of 150 update systems.
     */
    @Benchmark
    public void update150SystemsParallel(Blackhole bh) {
        parallelScheduler.execute(world);
        bh.consume(world);
    }

    /**
     * Benchmark: Measure stage computation time for 200 systems.
     */
    @Benchmark
    public void stagingOverhead200Systems(Blackhole bh) {
        SystemScheduler.Builder builder = new SystemScheduler.Builder();
        startupSystems.forEach(builder::addSystem);
        updateSystems.forEach(builder::addSystem);

        SystemScheduler scheduler = builder.build();
        bh.consume(scheduler.getStages());
        bh.consume(scheduler.getStartupStages());
        bh.consume(scheduler.getUpdateStages());
        scheduler.shutdown();
    }
}
