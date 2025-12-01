package io.github.ilumar589.jecs.benchmark;

import io.github.ilumar589.jecs.component.Health;
import io.github.ilumar589.jecs.component.Position;
import io.github.ilumar589.jecs.component.Velocity;
import io.github.ilumar589.jecs.system.System;
import io.github.ilumar589.jecs.system.SystemScheduler;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for SystemScheduler stage computation and building.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class SchedulerBenchmark {

    private System physics;
    private System health;
    private System render;
    private System ai;
    private System audio;
    private System input;
    private System collision;
    private System animation;

    @Setup(Level.Trial)
    public void setup() {
        physics = new System.Builder("Physics")
                .withMutable(Position.class)
                .withReadOnly(Velocity.class)
                .execute((w, q) -> {})
                .build();

        health = new System.Builder("Health")
                .withMutable(Health.class)
                .execute((w, q) -> {})
                .build();

        render = new System.Builder("Render")
                .withReadOnly(Position.class)
                .execute((w, q) -> {})
                .build();

        ai = new System.Builder("AI")
                .withMutable(Velocity.class)
                .execute((w, q) -> {})
                .build();

        audio = new System.Builder("Audio")
                .execute((w, q) -> {})
                .build();

        input = new System.Builder("Input")
                .execute((w, q) -> {})
                .build();

        collision = new System.Builder("Collision")
                .withReadOnly(Position.class)
                .withMutable(Velocity.class)
                .execute((w, q) -> {})
                .build();

        animation = new System.Builder("Animation")
                .withReadOnly(Position.class)
                .withReadOnly(Velocity.class)
                .execute((w, q) -> {})
                .build();
    }

    @Benchmark
    public void schedulerBuild_3Systems(Blackhole bh) {
        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(physics, health, render)
                .build();
        bh.consume(scheduler);
        scheduler.shutdown();
    }

    @Benchmark
    public void schedulerBuild_6Systems(Blackhole bh) {
        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(physics, health, render, ai, audio, input)
                .build();
        bh.consume(scheduler);
        scheduler.shutdown();
    }

    @Benchmark
    public void schedulerBuild_8SystemsWithSequence(Blackhole bh) {
        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(physics, health, render, ai, audio, input, collision, animation)
                .runInSequence(input, physics, render)
                .build();
        bh.consume(scheduler);
        scheduler.shutdown();
    }

    @Benchmark
    public void schedulerBuild_ComplexDependencies(Blackhole bh) {
        SystemScheduler scheduler = new SystemScheduler.Builder()
                .addSystems(physics, health, render, ai, audio, input, collision, animation)
                .runInSequence(input, ai, physics)
                .runInSequence(physics, collision, render)
                .build();
        bh.consume(scheduler);
        scheduler.shutdown();
    }
}
