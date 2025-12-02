package io.github.ilumar589.jecs.benchmark;

import io.github.ilumar589.jecs.component.Health;
import io.github.ilumar589.jecs.component.Position;
import io.github.ilumar589.jecs.component.Velocity;
import io.github.ilumar589.jecs.query.Mutable;
import io.github.ilumar589.jecs.query.ReadOnly;
import io.github.ilumar589.jecs.system.System;
import io.github.ilumar589.jecs.system.SystemScheduler;
import io.github.ilumar589.jecs.world.EcsWorld;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for the Systems API.
 * Tests performance of system execution, scheduling, and parallelization.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class SystemBenchmark {

    @Param({"100", "1000", "10000"})
    private int entityCount;

    private EcsWorld world;
    private SystemScheduler sequentialScheduler;
    private SystemScheduler parallelScheduler;
    private SystemScheduler singleSystemScheduler;

    @Setup(Level.Trial)
    @SuppressWarnings("unchecked")
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

        // Create systems using the unified forEach API
        System physics = new System.Builder("Physics")
                .withMutable(Position.class)
                .withReadOnly(Velocity.class)
                .execute((w, q) -> {
                    q.forEach(wrappers -> {
                        Mutable<Position> pos = (Mutable<Position>) wrappers[0];
                        ReadOnly<Velocity> vel = (ReadOnly<Velocity>) wrappers[1];
                        pos.set(new Position(
                                pos.get().x() + vel.get().dx(),
                                pos.get().y() + vel.get().dy(),
                                pos.get().z() + vel.get().dz()
                        ));
                    }, Position.class, Velocity.class);
                })
                .build();

        System health = new System.Builder("Health")
                .withMutable(Health.class)
                .execute((w, q) -> {
                    q.forEach(wrappers -> {
                        Mutable<Health> h = (Mutable<Health>) wrappers[0];
                        if (h.get().current() < h.get().max()) {
                            h.set(new Health(h.get().current() + 1, h.get().max()));
                        }
                    }, Health.class);
                })
                .build();

        System render = new System.Builder("Render")
                .withReadOnly(Position.class)
                .execute((w, q) -> {
                    q.forEach(Position.class, pos -> {
                        // Simulate rendering work
                        double x = pos.x();
                        double y = pos.y();
                    });
                })
                .build();

        // Sequential scheduler
        sequentialScheduler = new SystemScheduler.Builder()
                .addSystems(physics, health, render)
                .parallel(false)
                .build();

        // Parallel scheduler (uses virtual threads)
        parallelScheduler = new SystemScheduler.Builder()
                .addSystems(physics, health, render)
                .parallel(true)
                .build();

        // Single system scheduler
        singleSystemScheduler = new SystemScheduler.Builder()
                .addSystem(physics)
                .build();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws InterruptedException {
        sequentialScheduler.shutdown();
        parallelScheduler.shutdown();
        singleSystemScheduler.shutdown();
    }

    @Benchmark
    public void sequentialExecution(Blackhole bh) {
        sequentialScheduler.execute(world);
        bh.consume(world);
    }

    @Benchmark
    public void parallelExecution(Blackhole bh) {
        parallelScheduler.execute(world);
        bh.consume(world);
    }

    @Benchmark
    public void singleSystemExecution(Blackhole bh) {
        singleSystemScheduler.execute(world);
        bh.consume(world);
    }
}
