package io.github.ilumar589.jecs.system;

import io.github.ilumar589.jecs.component.Health;
import io.github.ilumar589.jecs.component.Position;
import io.github.ilumar589.jecs.component.Velocity;
import io.github.ilumar589.jecs.query.Mutable;
import io.github.ilumar589.jecs.query.ReadOnly;
import io.github.ilumar589.jecs.world.EcsWorld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SystemScheduler.
 */
class SystemSchedulerTest {

    private EcsWorld world;
    private List<SystemScheduler> schedulers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        world = new EcsWorld();
        schedulers = new ArrayList<>();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        for (SystemScheduler scheduler : schedulers) {
            scheduler.shutdownAndAwait(5, TimeUnit.SECONDS);
        }
    }

    private void trackScheduler(SystemScheduler scheduler) {
        schedulers.add(scheduler);
    }

    // ==================== Single System Tests ====================

    @Test
    void singleSystemExecution() {
        world.spawn(new Position(0, 0, 0));

        AtomicInteger count = new AtomicInteger(0);

        System counter = new System.Builder("Counter")
                .withReadOnly(Position.class)
                .execute((w, q) -> {
                    q.forEach(Position.class, pos -> count.incrementAndGet());
                })
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(counter)
                .build();
        trackScheduler(scheduler);

        scheduler.execute(world);

        assertEquals(1, count.get());
    }

    // ==================== Multiple Systems Tests ====================

    @Test
    void multipleSystemsInParallel() {
        world.spawn(new Position(0, 0, 0));
        world.spawn(new Velocity(1, 0, 0));

        AtomicInteger positionCount = new AtomicInteger(0);
        AtomicInteger velocityCount = new AtomicInteger(0);

        System positionSystem = new System.Builder("PositionSystem")
                .withReadOnly(Position.class)
                .execute((w, q) -> {
                    q.forEach(Position.class, pos -> positionCount.incrementAndGet());
                })
                .build();

        System velocitySystem = new System.Builder("VelocitySystem")
                .withReadOnly(Velocity.class)
                .execute((w, q) -> {
                    q.forEach(Velocity.class, vel -> velocityCount.incrementAndGet());
                })
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(positionSystem)
                .addSystem(velocitySystem)
                .build();
        trackScheduler(scheduler);

        scheduler.execute(world);

        assertEquals(1, positionCount.get());
        assertEquals(1, velocityCount.get());

        // Both systems should be in the same stage (no conflicts)
        assertEquals(1, scheduler.getStages().size());
        assertEquals(2, scheduler.getStages().get(0).size());
    }

    // ==================== Explicit Dependency Tests ====================

    @Test
    void explicitSequenceOrdering() {
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        System first = new System.Builder("First")
                .execute((w, q) -> executionOrder.add("First"))
                .build();

        System second = new System.Builder("Second")
                .execute((w, q) -> executionOrder.add("Second"))
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(first)
                .addSystem(second)
                .runInSequence(first, second) // First -> Second
                .build();
        trackScheduler(scheduler);

        scheduler.execute(world);

        assertEquals(2, executionOrder.size());
        assertEquals("First", executionOrder.get(0));
        assertEquals("Second", executionOrder.get(1));
    }

    @Test
    void runInSequenceWithThreeSystems() {
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        System a = new System.Builder("A").execute((w, q) -> executionOrder.add("A")).build();
        System b = new System.Builder("B").execute((w, q) -> executionOrder.add("B")).build();
        System c = new System.Builder("C").execute((w, q) -> executionOrder.add("C")).build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(a)
                .addSystem(b)
                .addSystem(c)
                .runInSequence(a, b, c) // A -> B -> C
                .build();
        trackScheduler(scheduler);

        scheduler.execute(world);

        assertEquals(3, executionOrder.size());
        assertEquals("A", executionOrder.get(0));
        assertEquals("B", executionOrder.get(1));
        assertEquals("C", executionOrder.get(2));
    }

    @Test
    void systemNotInSequenceRunsInParallel() {
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        System a = new System.Builder("A").execute((w, q) -> executionOrder.add("A")).build();
        System b = new System.Builder("B").execute((w, q) -> executionOrder.add("B")).build();
        System parallel = new System.Builder("Parallel").execute((w, q) -> executionOrder.add("Parallel")).build();
        System c = new System.Builder("C").execute((w, q) -> executionOrder.add("C")).build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(a, b, parallel, c)
                .runInSequence(a, b, c) // A -> B -> C, Parallel can run with any
                .build();
        trackScheduler(scheduler);

        // Parallel should be in the same stage as A (first stage with no dependencies)
        assertEquals(3, scheduler.getStages().size()); // A+Parallel, B, C

        scheduler.execute(world);
        assertEquals(4, executionOrder.size());
    }

    // ==================== Automatic Conflict-Based Staging Tests ====================

    @Test
    void automaticConflictBasedStaging() {
        // Physics writes Position, Render reads Position - they conflict
        System physics = new System.Builder("Physics")
                .withMutable(Position.class)
                .withReadOnly(Velocity.class)
                .execute((w, q) -> {})
                .build();

        System render = new System.Builder("Render")
                .withReadOnly(Position.class)
                .execute((w, q) -> {})
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(physics)
                .addSystem(render)
                .build();
        trackScheduler(scheduler);

        // Systems should be in different stages due to conflict
        assertEquals(2, scheduler.getStages().size());
        assertEquals(1, scheduler.getStages().get(0).size());
        assertEquals(1, scheduler.getStages().get(1).size());
    }

    @Test
    void physicsAndRenderingExample() {
        world.spawn(new Position(0, 0, 0), new Velocity(1, 2, 3));

        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        // Physics: reads Velocity, writes Position
        System physics = new System.Builder("Physics")
                .withMutable(Position.class)
                .withReadOnly(Velocity.class)
                .execute((w, q) -> {
                    executionOrder.add("Physics");
                    q.forEach(wrappers -> {
                        @SuppressWarnings("unchecked")
                        Mutable<Position> pos = (Mutable<Position>) wrappers[0];
                        @SuppressWarnings("unchecked")
                        ReadOnly<Velocity> vel = (ReadOnly<Velocity>) wrappers[1];
                        pos.set(new Position(
                                pos.get().x() + vel.get().dx(),
                                pos.get().y() + vel.get().dy(),
                                pos.get().z() + vel.get().dz()
                        ));
                    }, Position.class, Velocity.class);
                })
                .build();

        // Render: reads Position (must run after Physics)
        System render = new System.Builder("Render")
                .withReadOnly(Position.class)
                .execute((w, q) -> {
                    executionOrder.add("Render");
                })
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(physics)
                .addSystem(render)
                .build();
        trackScheduler(scheduler);

        scheduler.execute(world);

        // Physics must run before Render
        assertEquals(2, executionOrder.size());
        assertEquals("Physics", executionOrder.get(0));
        assertEquals("Render", executionOrder.get(1));

        // Verify position was updated
        world.componentQuery()
                .forEach(Position.class, pos -> {
                    assertEquals(1.0f, pos.x());
                    assertEquals(2.0f, pos.y());
                    assertEquals(3.0f, pos.z());
                });
    }

    // ==================== Circular Dependency Tests ====================

    @Test
    void circularDependencyDetection() {
        System a = new System.Builder("A").execute((w, q) -> {}).build();
        System b = new System.Builder("B").execute((w, q) -> {}).build();
        System c = new System.Builder("C").execute((w, q) -> {}).build();

        assertThrows(IllegalStateException.class, () -> {
            new SystemScheduler.Builder()
                    .addSystem(a)
                    .addSystem(b)
                    .addSystem(c)
                    .runInSequence(a, b, c, a) // Creates cycle: A -> B -> C -> A
                    .build();
        });
    }

    @Test
    void selfDependencyDetection() {
        System a = new System.Builder("A").execute((w, q) -> {}).build();

        assertThrows(IllegalStateException.class, () -> {
            new SystemScheduler.Builder()
                    .addSystem(a)
                    .runInSequence(a, a) // A depends on itself
                    .build();
        });
    }

    // ==================== Sequential Execution Mode Tests ====================

    @Test
    void sequentialExecutionMode() {
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        System a = new System.Builder("A")
                .execute((w, q) -> {
                    try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                    executionOrder.add("A");
                })
                .build();

        System b = new System.Builder("B")
                .execute((w, q) -> {
                    executionOrder.add("B");
                })
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(a)
                .addSystem(b)
                .parallel(false) // Sequential mode
                .build();
        trackScheduler(scheduler);

        scheduler.execute(world);

        // Both should execute in order
        assertEquals(2, executionOrder.size());
        assertEquals("A", executionOrder.get(0));
        assertEquals("B", executionOrder.get(1));

        assertFalse(scheduler.isParallel());
    }

    // ==================== Custom Executor Tests ====================

    @Test
    void customExecutorService() {
        AtomicInteger count = new AtomicInteger(0);

        System system = new System.Builder("Counter")
                .execute((w, q) -> count.incrementAndGet())
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(system)
                .withExecutor(Executors.newSingleThreadExecutor())
                .build();
        trackScheduler(scheduler);

        scheduler.execute(world);

        assertEquals(1, count.get());
    }

    // ==================== Complex Dependency Chain Tests ====================

    @Test
    void complexDependencyChainWithSequence() {
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        // A -> B -> D and A -> C -> D using two sequences
        System a = new System.Builder("A").execute((w, q) -> executionOrder.add("A")).build();
        System b = new System.Builder("B").execute((w, q) -> executionOrder.add("B")).build();
        System c = new System.Builder("C").execute((w, q) -> executionOrder.add("C")).build();
        System d = new System.Builder("D").execute((w, q) -> executionOrder.add("D")).build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(a)
                .addSystem(b)
                .addSystem(c)
                .addSystem(d)
                .runInSequence(a, b, d)  // A -> B -> D
                .runInSequence(a, c, d)  // A -> C -> D
                .build();
        trackScheduler(scheduler);

        scheduler.execute(world);

        // A must be first, D must be last
        assertEquals(4, executionOrder.size());
        assertEquals("A", executionOrder.get(0));
        assertEquals("D", executionOrder.get(3));
        // B and C can be in any order between A and D
    }

    @Test
    void automaticParallelizationWithConflicts() {
        // Physics writes Position
        System physics = new System.Builder("Physics")
                .withMutable(Position.class)
                .execute((w, q) -> {})
                .build();

        // AI reads Position (implicit dependency on Physics - must run after)
        System ai = new System.Builder("AI")
                .withReadOnly(Position.class)
                .execute((w, q) -> {})
                .build();

        // Render also reads Position (can run in parallel with AI)
        System render = new System.Builder("Render")
                .withReadOnly(Position.class)
                .execute((w, q) -> {})
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(physics)
                .addSystem(ai)
                .addSystem(render)
                .build();
        trackScheduler(scheduler);

        // Should have 2 stages: Physics alone, then AI and Render together
        assertEquals(2, scheduler.getStages().size());
        assertEquals(1, scheduler.getStages().get(0).size()); // Physics
        assertEquals(2, scheduler.getStages().get(1).size()); // AI + Render in parallel
    }

    // ==================== Stage Tests ====================

    @Test
    void stagesAreCorrectlyComputed() {
        // No conflicts, should be single stage
        System a = new System.Builder("A")
                .withReadOnly(Position.class)
                .execute((w, q) -> {})
                .build();

        System b = new System.Builder("B")
                .withReadOnly(Velocity.class)
                .execute((w, q) -> {})
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(a)
                .addSystem(b)
                .build();
        trackScheduler(scheduler);

        var stages = scheduler.getStages();
        assertEquals(1, stages.size());
        assertEquals(2, stages.get(0).size());
    }

    @Test
    void emptyScheduler() {
        SystemScheduler scheduler = new SystemScheduler.Builder().build();
        trackScheduler(scheduler);

        // Should not throw
        scheduler.execute(world);

        assertTrue(scheduler.getStages().isEmpty());
    }

    // ==================== Shutdown Tests ====================

    @Test
    void shutdownStopsExecutor() {
        System system = new System.Builder("Test")
                .execute((w, q) -> {})
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(system)
                .build();

        scheduler.shutdown();
        // Should not throw even after shutdown
    }

    @Test
    void shutdownAndAwaitWithTimeout() throws InterruptedException {
        System system = new System.Builder("Test")
                .execute((w, q) -> {})
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(system)
                .build();

        boolean terminated = scheduler.shutdownAndAwait(1, TimeUnit.SECONDS);
        assertTrue(terminated);
    }

    // ==================== AddSystems Varargs Tests ====================

    @Test
    void addSystemsVarargs() {
        System a = new System.Builder("A").execute((w, q) -> {}).build();
        System b = new System.Builder("B").execute((w, q) -> {}).build();
        System c = new System.Builder("C").execute((w, q) -> {}).build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(a, b, c)
                .build();
        trackScheduler(scheduler);

        // All should be in the same stage (no conflicts)
        assertEquals(1, scheduler.getStages().size());
        assertEquals(3, scheduler.getStages().get(0).size());
    }

    // ==================== Real World Scenario Tests ====================

    @Test
    void gameLoopExample() {
        world.spawn(new Position(0, 0, 0), new Velocity(1, 0, 0), new Health(100, 100));
        world.spawn(new Position(10, 0, 0), new Velocity(-1, 0, 0), new Health(50, 100));

        // Input system - reads nothing, modifies nothing in this example
        System input = new System.Builder("Input")
                .execute((w, q) -> {
                    // Would process input events here
                })
                .build();

        // Physics system - reads Velocity, writes Position
        System physics = new System.Builder("Physics")
                .withMutable(Position.class)
                .withReadOnly(Velocity.class)
                .execute((w, q) -> {
                    q.forEach(wrappers -> {
                        @SuppressWarnings("unchecked")
                        Mutable<Position> pos = (Mutable<Position>) wrappers[0];
                        @SuppressWarnings("unchecked")
                        ReadOnly<Velocity> vel = (ReadOnly<Velocity>) wrappers[1];
                        pos.set(new Position(
                                pos.get().x() + vel.get().dx(),
                                pos.get().y() + vel.get().dy(),
                                pos.get().z() + vel.get().dz()
                        ));
                    }, Position.class, Velocity.class);
                })
                .build();

        // Health system - reads and writes Health (no conflict with physics or render)
        System health = new System.Builder("Health")
                .withMutable(Health.class)
                .execute((w, q) -> {
                    q.forEach(wrappers -> {
                        @SuppressWarnings("unchecked")
                        Mutable<Health> h = (Mutable<Health>) wrappers[0];
                        // Regenerate 1 health per tick
                        if (h.get().current() < h.get().max()) {
                            h.set(new Health(h.get().current() + 1, h.get().max()));
                        }
                    }, Health.class);
                })
                .build();

        // Render system - reads Position (automatic dependency on physics due to conflict)
        System render = new System.Builder("Render")
                .withReadOnly(Position.class)
                .execute((w, q) -> {
                    // Would render entities here
                })
                .build();

        // The scheduler automatically determines:
        // - Input has no conflicts, can run first
        // - Physics writes Position, must run before Render (which reads Position)
        // - Health has no conflicts with others, can run in parallel
        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(input, physics, health, render)
                .runInSequence(input, physics) // Explicit: physics after input
                .build();
        trackScheduler(scheduler);

        // Execute one "frame"
        scheduler.execute(world);

        // Verify physics updated positions
        List<Float> xPositions = new ArrayList<>();
        world.componentQuery()
                .forEach(Position.class, pos -> xPositions.add(pos.x()));

        assertTrue(xPositions.contains(1.0f));  // 0 + 1
        assertTrue(xPositions.contains(9.0f));  // 10 + (-1)

        // Verify health regenerated
        List<Integer> healthValues = new ArrayList<>();
        world.componentQuery()
                .forEach(Health.class, h -> healthValues.add(h.current()));

        assertTrue(healthValues.contains(100)); // Already max
        assertTrue(healthValues.contains(51));  // 50 + 1
    }

    @Test
    void virtualThreadsAreUsedByDefault() {
        AtomicInteger count = new AtomicInteger(0);

        System system = new System.Builder("Counter")
                .execute((w, q) -> count.incrementAndGet())
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(system)
                .build();
        trackScheduler(scheduler);

        scheduler.execute(world);

        assertEquals(1, count.get());
        assertTrue(scheduler.isParallel());
    }

    // ==================== SystemMode Tests ====================

    @Test
    void startupSystemsOnlyRunDuringExecuteStartup() {
        AtomicInteger startupCount = new AtomicInteger(0);
        AtomicInteger updateCount = new AtomicInteger(0);

        System startup = new System.Builder("Startup")
                .mode(SystemMode.STARTUP)
                .execute((w, q) -> startupCount.incrementAndGet())
                .build();

        System update = new System.Builder("Update")
                .mode(SystemMode.UPDATE)
                .execute((w, q) -> updateCount.incrementAndGet())
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(startup, update)
                .build();
        trackScheduler(scheduler);

        scheduler.executeStartup(world);

        assertEquals(1, startupCount.get());
        assertEquals(0, updateCount.get());
    }

    @Test
    void updateSystemsOnlyRunDuringExecuteUpdate() {
        AtomicInteger startupCount = new AtomicInteger(0);
        AtomicInteger updateCount = new AtomicInteger(0);

        System startup = new System.Builder("Startup")
                .mode(SystemMode.STARTUP)
                .execute((w, q) -> startupCount.incrementAndGet())
                .build();

        System update = new System.Builder("Update")
                .mode(SystemMode.UPDATE)
                .execute((w, q) -> updateCount.incrementAndGet())
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(startup, update)
                .build();
        trackScheduler(scheduler);

        scheduler.executeUpdate(world);

        assertEquals(0, startupCount.get());
        assertEquals(1, updateCount.get());
    }

    @Test
    void shutdownSystemsOnlyRunDuringExecuteShutdown() {
        AtomicInteger shutdownCount = new AtomicInteger(0);
        AtomicInteger updateCount = new AtomicInteger(0);

        System shutdown = new System.Builder("Shutdown")
                .mode(SystemMode.SHUTDOWN)
                .execute((w, q) -> shutdownCount.incrementAndGet())
                .build();

        System update = new System.Builder("Update")
                .mode(SystemMode.UPDATE)
                .execute((w, q) -> updateCount.incrementAndGet())
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(shutdown, update)
                .build();
        trackScheduler(scheduler);

        scheduler.executeShutdown(world);

        assertEquals(1, shutdownCount.get());
        assertEquals(0, updateCount.get());
    }

    @Test
    void mixedStartupAndUpdateSystemsHandledCorrectly() {
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        System startup1 = new System.Builder("Startup1")
                .mode(SystemMode.STARTUP)
                .execute((w, q) -> executionOrder.add("Startup1"))
                .build();

        System startup2 = new System.Builder("Startup2")
                .mode(SystemMode.STARTUP)
                .execute((w, q) -> executionOrder.add("Startup2"))
                .build();

        System update1 = new System.Builder("Update1")
                .mode(SystemMode.UPDATE)
                .execute((w, q) -> executionOrder.add("Update1"))
                .build();

        System update2 = new System.Builder("Update2")
                .mode(SystemMode.UPDATE)
                .execute((w, q) -> executionOrder.add("Update2"))
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(startup1, startup2, update1, update2)
                .build();
        trackScheduler(scheduler);

        // Execute startup
        scheduler.executeStartup(world);
        assertEquals(2, executionOrder.size());
        assertTrue(executionOrder.contains("Startup1"));
        assertTrue(executionOrder.contains("Startup2"));

        executionOrder.clear();

        // Execute update
        scheduler.executeUpdate(world);
        assertEquals(2, executionOrder.size());
        assertTrue(executionOrder.contains("Update1"));
        assertTrue(executionOrder.contains("Update2"));
    }

    @Test
    void executeAllRunsAllSystems() {
        AtomicInteger startupCount = new AtomicInteger(0);
        AtomicInteger updateCount = new AtomicInteger(0);
        AtomicInteger shutdownCount = new AtomicInteger(0);

        System startup = new System.Builder("Startup")
                .mode(SystemMode.STARTUP)
                .execute((w, q) -> startupCount.incrementAndGet())
                .build();

        System update = new System.Builder("Update")
                .mode(SystemMode.UPDATE)
                .execute((w, q) -> updateCount.incrementAndGet())
                .build();

        System shutdown = new System.Builder("Shutdown")
                .mode(SystemMode.SHUTDOWN)
                .execute((w, q) -> shutdownCount.incrementAndGet())
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(startup, update, shutdown)
                .build();
        trackScheduler(scheduler);

        scheduler.execute(world);

        assertEquals(1, startupCount.get());
        assertEquals(1, updateCount.get());
        assertEquals(1, shutdownCount.get());
    }

    @Test
    void backwardCompatibilityWithoutMode() {
        AtomicInteger count = new AtomicInteger(0);

        // System created without explicit mode (should default to UPDATE)
        System system = new System.Builder("Legacy")
                .execute((w, q) -> count.incrementAndGet())
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(system)
                .build();
        trackScheduler(scheduler);

        // executeUpdate should run the system
        scheduler.executeUpdate(world);
        assertEquals(1, count.get());

        // executeStartup should not run the system
        count.set(0);
        scheduler.executeStartup(world);
        assertEquals(0, count.get());
    }

    @Test
    void startupSystemsRespectDependenciesAndStaging() {
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        // s1 writes Position, s2 reads Position -> conflict, must be staged
        System s1 = new System.Builder("StartupWriter")
                .mode(SystemMode.STARTUP)
                .withMutable(Position.class)
                .execute((w, q) -> executionOrder.add("StartupWriter"))
                .build();

        System s2 = new System.Builder("StartupReader")
                .mode(SystemMode.STARTUP)
                .withReadOnly(Position.class)
                .execute((w, q) -> executionOrder.add("StartupReader"))
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(s1, s2)
                .build();
        trackScheduler(scheduler);

        // Should be in 2 stages due to conflict
        assertEquals(2, scheduler.getStartupStages().size());

        scheduler.executeStartup(world);

        assertEquals(2, executionOrder.size());
        assertEquals("StartupWriter", executionOrder.get(0));
        assertEquals("StartupReader", executionOrder.get(1));
    }

    @Test
    void stagingWorksCorrectlyWithinEachMode() {
        // Create systems with conflicts within the same mode
        System startupWriter = new System.Builder("StartupWriter")
                .mode(SystemMode.STARTUP)
                .withMutable(Position.class)
                .execute((w, q) -> {})
                .build();

        System startupReader = new System.Builder("StartupReader")
                .mode(SystemMode.STARTUP)
                .withReadOnly(Position.class)
                .execute((w, q) -> {})
                .build();

        System startupNoConflict = new System.Builder("StartupNoConflict")
                .mode(SystemMode.STARTUP)
                .withMutable(Velocity.class)
                .execute((w, q) -> {})
                .build();

        System updateWriter = new System.Builder("UpdateWriter")
                .mode(SystemMode.UPDATE)
                .withMutable(Health.class)
                .execute((w, q) -> {})
                .build();

        System updateReader = new System.Builder("UpdateReader")
                .mode(SystemMode.UPDATE)
                .withReadOnly(Health.class)
                .execute((w, q) -> {})
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(startupWriter, startupReader, startupNoConflict, updateWriter, updateReader)
                .build();
        trackScheduler(scheduler);

        // Startup: startupWriter conflicts with startupReader, startupNoConflict can run with either
        // So: Stage 1: [startupWriter, startupNoConflict], Stage 2: [startupReader]
        // or Stage 1: [startupWriter], Stage 2: [startupReader, startupNoConflict]
        assertEquals(2, scheduler.getStartupStages().size());

        // Update: updateWriter conflicts with updateReader
        assertEquals(2, scheduler.getUpdateStages().size());
    }

    @Test
    void getStartupStagesReturnsCorrectStages() {
        System startup = new System.Builder("Startup")
                .mode(SystemMode.STARTUP)
                .execute((w, q) -> {})
                .build();

        System update = new System.Builder("Update")
                .mode(SystemMode.UPDATE)
                .execute((w, q) -> {})
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(startup, update)
                .build();
        trackScheduler(scheduler);

        List<SystemStage> startupStages = scheduler.getStartupStages();
        assertEquals(1, startupStages.size());
        assertEquals(1, startupStages.get(0).size());
        assertEquals("Startup", startupStages.get(0).getSystems().get(0).getName());
    }

    @Test
    void getUpdateStagesReturnsCorrectStages() {
        System startup = new System.Builder("Startup")
                .mode(SystemMode.STARTUP)
                .execute((w, q) -> {})
                .build();

        System update = new System.Builder("Update")
                .mode(SystemMode.UPDATE)
                .execute((w, q) -> {})
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(startup, update)
                .build();
        trackScheduler(scheduler);

        List<SystemStage> updateStages = scheduler.getUpdateStages();
        assertEquals(1, updateStages.size());
        assertEquals(1, updateStages.get(0).size());
        assertEquals("Update", updateStages.get(0).getSystems().get(0).getName());
    }

    @Test
    void getShutdownStagesReturnsCorrectStages() {
        System update = new System.Builder("Update")
                .mode(SystemMode.UPDATE)
                .execute((w, q) -> {})
                .build();

        System shutdown = new System.Builder("Shutdown")
                .mode(SystemMode.SHUTDOWN)
                .execute((w, q) -> {})
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(update, shutdown)
                .build();
        trackScheduler(scheduler);

        List<SystemStage> shutdownStages = scheduler.getShutdownStages();
        assertEquals(1, shutdownStages.size());
        assertEquals(1, shutdownStages.get(0).size());
        assertEquals("Shutdown", shutdownStages.get(0).getSystems().get(0).getName());
    }

    @Test
    void emptyStartupStagesWhenNoStartupSystems() {
        System update = new System.Builder("Update")
                .mode(SystemMode.UPDATE)
                .execute((w, q) -> {})
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystem(update)
                .build();
        trackScheduler(scheduler);

        assertTrue(scheduler.getStartupStages().isEmpty());
    }

    @Test
    void gameLifecycleScenario() {
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        // Startup systems
        System resourceLoader = new System.Builder("ResourceLoader")
                .mode(SystemMode.STARTUP)
                .execute((w, q) -> executionOrder.add("ResourceLoader"))
                .build();

        System worldInit = new System.Builder("WorldInit")
                .mode(SystemMode.STARTUP)
                .execute((w, q) -> executionOrder.add("WorldInit"))
                .build();

        // Update systems
        System physics = new System.Builder("Physics")
                .mode(SystemMode.UPDATE)
                .execute((w, q) -> executionOrder.add("Physics"))
                .build();

        System render = new System.Builder("Render")
                .mode(SystemMode.UPDATE)
                .execute((w, q) -> executionOrder.add("Render"))
                .build();

        // Shutdown systems
        System saveGame = new System.Builder("SaveGame")
                .mode(SystemMode.SHUTDOWN)
                .execute((w, q) -> executionOrder.add("SaveGame"))
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(resourceLoader, worldInit, physics, render, saveGame)
                .build();
        trackScheduler(scheduler);

        // Simulate game lifecycle
        scheduler.executeStartup(world);
        assertTrue(executionOrder.contains("ResourceLoader"));
        assertTrue(executionOrder.contains("WorldInit"));
        assertEquals(2, executionOrder.size());

        executionOrder.clear();

        // Simulate 3 game loop frames
        for (int i = 0; i < 3; i++) {
            scheduler.executeUpdate(world);
        }
        assertEquals(6, executionOrder.size()); // 2 update systems * 3 frames

        executionOrder.clear();

        scheduler.executeShutdown(world);
        assertEquals(1, executionOrder.size());
        assertEquals("SaveGame", executionOrder.get(0));
    }

    @Test
    void runInSequenceWorksWithinSameMode() {
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        System s1 = new System.Builder("S1")
                .mode(SystemMode.STARTUP)
                .execute((w, q) -> executionOrder.add("S1"))
                .build();

        System s2 = new System.Builder("S2")
                .mode(SystemMode.STARTUP)
                .execute((w, q) -> executionOrder.add("S2"))
                .build();

        System s3 = new System.Builder("S3")
                .mode(SystemMode.STARTUP)
                .execute((w, q) -> executionOrder.add("S3"))
                .build();

        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(s1, s2, s3)
                .runInSequence(s1, s2, s3)
                .build();
        trackScheduler(scheduler);

        scheduler.executeStartup(world);

        assertEquals(3, executionOrder.size());
        assertEquals("S1", executionOrder.get(0));
        assertEquals("S2", executionOrder.get(1));
        assertEquals("S3", executionOrder.get(2));
    }
}
