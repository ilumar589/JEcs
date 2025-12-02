package io.github.ilumar589.jecs.benchmark;

import io.github.ilumar589.jecs.component.Health;
import io.github.ilumar589.jecs.component.Position;
import io.github.ilumar589.jecs.component.Velocity;
import io.github.ilumar589.jecs.query.Mutable;
import io.github.ilumar589.jecs.query.ReadOnly;
import io.github.ilumar589.jecs.world.EcsWorld;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for query performance optimizations.
 * 
 * <h2>Optimizations Tested</h2>
 * <ul>
 *   <li><b>Wrapper Reuse:</b> Reusing Mutable/ReadOnly wrappers instead of allocating per-entity</li>
 *   <li><b>Query Caching:</b> Caching matching archetypes for repeated queries</li>
 *   <li><b>Bitset Matching:</b> O(1) bitwise operations instead of O(n×m) set operations</li>
 *   <li><b>Reader/Writer Caching:</b> Caching ComponentReader/Writer per archetype</li>
 * </ul>
 * 
 * <h2>Expected Results</h2>
 * <ul>
 *   <li>Query performance should improve by at least 50% for repeated queries (caching)</li>
 *   <li>Memory allocation in hot paths should reduce by at least 80% (wrapper reuse)</li>
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class QueryPerformanceBenchmark {

    @Param({"1000", "10000", "100000"})
    private int entityCount;

    private EcsWorld world;
    
    // Volatile sink to prevent dead code elimination
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
    }

    /**
     * Benchmark: Simple read-only query with one component.
     * Tests basic query iteration performance.
     */
    @Benchmark
    public void simpleReadOnlyQuery(Blackhole bh) {
        world.componentQuery()
            .forEach(Position.class, pos -> {
                sink = pos.x() + pos.y() + pos.z();
            });
        bh.consume(sink);
    }

    /**
     * Benchmark: Read-only query with two components.
     * Tests performance with multiple component access.
     */
    @Benchmark
    public void twoComponentReadOnlyQuery(Blackhole bh) {
        world.componentQuery()
            .forEach(Position.class, Velocity.class, (pos, vel) -> {
                sink = pos.x() + vel.dx();
            });
        bh.consume(sink);
    }

    /**
     * Benchmark: Mutable wrapper query with position update.
     * Tests wrapper reuse optimization.
     */
    @Benchmark
    @SuppressWarnings("unchecked")
    public void mutableWrapperQuery(Blackhole bh) {
        world.componentQuery()
            .withMutable(Position.class)
            .withReadOnly(Velocity.class)
            .forEach(wrappers -> {
                Mutable<Position> pos = (Mutable<Position>) wrappers[0];
                ReadOnly<Velocity> vel = (ReadOnly<Velocity>) wrappers[1];
                pos.update(p -> new Position(
                    p.x() + vel.get().dx(),
                    p.y() + vel.get().dy(),
                    p.z() + vel.get().dz()
                ));
            }, Position.class, Velocity.class);
        bh.consume(sink);
    }

    /**
     * Benchmark: Query with exclusion (without clause).
     * Tests bitset matching for exclusion checks.
     */
    @Benchmark
    public void queryWithExclusion(Blackhole bh) {
        world.componentQuery()
            .with(Position.class)
            .without(Health.class)
            .forEach(Position.class, pos -> {
                sink = pos.x() + pos.y();
            });
        bh.consume(sink);
    }

    /**
     * Benchmark: Repeated identical queries.
     * Tests query caching performance - should be significantly faster
     * on subsequent executions due to archetype cache hits.
     */
    @Benchmark
    public void repeatedIdenticalQueries(Blackhole bh) {
        // Execute the same query 10 times to measure cache benefits
        for (int i = 0; i < 10; i++) {
            int count = world.componentQuery()
                .with(Position.class, Velocity.class)
                .without(Health.class)
                .count();
            bh.consume(count);
        }
    }

    /**
     * Benchmark: Type-safe forEachWrapper with 3 components.
     * Tests wrapper allocation and type-safe API performance.
     */
    @Benchmark
    @SuppressWarnings("unchecked")
    public void typeSafeForEachWrapper(Blackhole bh) {
        world.componentQuery()
            .withMutable(Position.class, Health.class)
            .withReadOnly(Velocity.class)
            .forEachWrapper(Position.class, Velocity.class, Health.class, (pos, vel, health) -> {
                sink = pos.get().x() + vel.get().dx() + health.get().current();
            });
        bh.consume(sink);
    }

    /**
     * Benchmark: Count query (no iteration, just archetype matching).
     * Tests pure archetype matching performance.
     */
    @Benchmark
    public void countQuery(Blackhole bh) {
        int count = world.componentQuery()
            .with(Position.class, Velocity.class)
            .count();
        bh.consume(count);
    }

    /**
     * Benchmark: Any query (early termination on first match).
     * Tests archetype matching with early exit.
     */
    @Benchmark
    public void anyQuery(Blackhole bh) {
        boolean hasAny = world.componentQuery()
            .with(Position.class, Velocity.class, Health.class)
            .any();
        bh.consume(hasAny);
    }

    /**
     * Benchmark: Complex query with multiple conditions.
     * Tests combined performance of all optimizations.
     */
    @Benchmark
    @SuppressWarnings("unchecked")
    public void complexQuery(Blackhole bh) {
        world.componentQuery()
            .withMutable(Position.class)
            .withReadOnly(Velocity.class)
            .with(Health.class)
            .forEach(wrappers -> {
                Mutable<Position> pos = (Mutable<Position>) wrappers[0];
                ReadOnly<Velocity> vel = (ReadOnly<Velocity>) wrappers[1];
                
                // Simulate physics update
                Position p = pos.get();
                Velocity v = vel.get();
                pos.set(new Position(
                    p.x() + v.dx() * 0.016f,
                    p.y() + v.dy() * 0.016f,
                    p.z() + v.dz() * 0.016f
                ));
            }, Position.class, Velocity.class);
        bh.consume(sink);
    }

    /**
     * Benchmark: Query modification with predicate.
     * Tests modifyIf performance.
     */
    @Benchmark
    public void modifyIfQuery(Blackhole bh) {
        world.componentQuery()
            .modifyIf(Health.class,
                h -> h.current() < h.max(),
                h -> new Health(h.current() + 1, h.max())
            );
        bh.consume(sink);
    }
}
