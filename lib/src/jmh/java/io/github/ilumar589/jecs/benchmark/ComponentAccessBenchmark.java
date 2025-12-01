package io.github.ilumar589.jecs.benchmark;

import io.github.ilumar589.jecs.component.Health;
import io.github.ilumar589.jecs.component.Position;
import io.github.ilumar589.jecs.component.Velocity;
import io.github.ilumar589.jecs.system.ComponentAccess;
import io.github.ilumar589.jecs.system.System;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for ComponentAccess conflict detection.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class ComponentAccessBenchmark {

    private ComponentAccess readOnlyAccess;
    private ComponentAccess mutableAccess;
    private ComponentAccess mixedAccess;
    private ComponentAccess largeAccess;

    private System system1;
    private System system2;
    private System systemNoConflict;

    @Setup(Level.Trial)
    public void setup() {
        // Simple read-only access
        readOnlyAccess = new ComponentAccess.Builder()
                .addReadOnly(Position.class)
                .addReadOnly(Velocity.class)
                .build();

        // Mutable access
        mutableAccess = new ComponentAccess.Builder()
                .addMutable(Position.class)
                .addReadOnly(Velocity.class)
                .build();

        // Mixed access with exclusions
        mixedAccess = new ComponentAccess.Builder()
                .addReadOnly(Velocity.class)
                .addMutable(Position.class)
                .addExcluded(Health.class)
                .build();

        // Larger access pattern
        largeAccess = new ComponentAccess.Builder()
                .addReadOnly(Position.class, Velocity.class, Health.class)
                .addMutable(Position.class)
                .addExcluded(Health.class)
                .build();

        // Systems for conflict detection
        system1 = new System.Builder("System1")
                .withMutable(Position.class)
                .withReadOnly(Velocity.class)
                .execute((w, q) -> {})
                .build();

        system2 = new System.Builder("System2")
                .withReadOnly(Position.class)
                .execute((w, q) -> {})
                .build();

        systemNoConflict = new System.Builder("SystemNoConflict")
                .withMutable(Health.class)
                .execute((w, q) -> {})
                .build();
    }

    @Benchmark
    public void conflictDetection_Conflict(Blackhole bh) {
        bh.consume(mutableAccess.conflictsWith(readOnlyAccess));
    }

    @Benchmark
    public void conflictDetection_NoConflict(Blackhole bh) {
        bh.consume(readOnlyAccess.conflictsWith(readOnlyAccess));
    }

    @Benchmark
    public void systemConflictCheck_Conflict(Blackhole bh) {
        bh.consume(system1.conflictsWith(system2));
    }

    @Benchmark
    public void systemConflictCheck_NoConflict(Blackhole bh) {
        bh.consume(system1.conflictsWith(systemNoConflict));
    }

    @Benchmark
    public void accessBuilderCreation(Blackhole bh) {
        ComponentAccess access = new ComponentAccess.Builder()
                .addReadOnly(Position.class)
                .addMutable(Velocity.class)
                .addExcluded(Health.class)
                .build();
        bh.consume(access);
    }

    @Benchmark
    public void systemBuilderCreation(Blackhole bh) {
        System system = new System.Builder("TestSystem")
                .withMutable(Position.class)
                .withReadOnly(Velocity.class)
                .without(Health.class)
                .execute((w, q) -> {})
                .build();
        bh.consume(system);
    }
}
