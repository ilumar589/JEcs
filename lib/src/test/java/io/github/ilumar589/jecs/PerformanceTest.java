package io.github.ilumar589.jecs;

import io.github.ilumar589.jecs.component.*;
import io.github.ilumar589.jecs.entity.Entity;
import io.github.ilumar589.jecs.world.EcsWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Performance tests for the JEcs ECS framework.
 * Tests measure the execution time of core component operations at various scales.
 * 
 * <h2>JIT Warmup</h2>
 * Each test includes warmup iterations to ensure JIT compilation is complete
 * before measuring. This provides more accurate and stable performance measurements.
 */
class PerformanceTest {

    /**
     * Number of warmup iterations to trigger JIT compilation.
     * Each warmup iteration processes a fraction of the full dataset multiple times,
     * ensuring hot paths are compiled before measurement.
     */
    private static final int WARMUP_ITERATIONS = 5;
    
    /**
     * Factor to reduce dataset size during warmup (1/WARMUP_SIZE_FACTOR of full size).
     * This speeds up warmup while still exercising all code paths.
     */
    private static final int WARMUP_SIZE_FACTOR = 10;
    
    /**
     * Number of query iterations during warmup to ensure query paths are JIT compiled.
     */
    private static final int WARMUP_QUERY_ITERATIONS = 100;
    
    /**
     * Number of measurement iterations for averaging results.
     */
    private static final int MEASUREMENT_ITERATIONS = 3;

    private EcsWorld world;

    @BeforeEach
    void setUp() {
        world = new EcsWorld();
    }
    
    // ==================== JIT Warmup Helper ====================
    
    /**
     * Performs JIT warmup by running the given operation multiple times.
     * This ensures the JVM has compiled hot paths before measurement.
     * 
     * <p>The warmup runs the operation {@link #WARMUP_ITERATIONS} times,
     * which combined with the operation-internal loops should trigger
     * JIT compilation thresholds.</p>
     *
     * @param warmupOperation the operation to warm up
     */
    private void performWarmup(Runnable warmupOperation) {
        System.out.println("  Warming up JIT (" + WARMUP_ITERATIONS + " iterations)...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            // Reset world between warmup iterations
            world = new EcsWorld();
            warmupOperation.run();
        }
        // Final reset for measurement
        world = new EcsWorld();
        System.out.println("  Warmup complete.");
    }

    // ==================== Add Component Performance Tests ====================

    @Test
    void performanceAddComponents1000() {
        int count = 1_000;
        measureAddComponentPerformance(count);
    }

    @Test
    void performanceAddComponents10000() {
        int count = 10_000;
        measureAddComponentPerformance(count);
    }

    @Test
    void performanceAddComponents100000() {
        int count = 100_000;
        measureAddComponentPerformance(count);
    }

    @Test
    void performanceAddComponents1000000() {
        int count = 1_000_000;
        measureAddComponentPerformance(count);
    }

    private void measureAddComponentPerformance(int count) {
        System.out.println("\n=== Add Component Performance Test (n=" + count + ") ===");
        
        // Warmup phase
        int warmupSize = Math.max(1, count / WARMUP_SIZE_FACTOR);
        performWarmup(() -> {
            List<Entity> entities = createEntitiesWithPosition(warmupSize);
            for (int i = 0; i < entities.size(); i++) {
                world.addComponent(entities.get(i), new Velocity(1.0f, 1.0f, 1.0f));
            }
        });
        
        // Measurement phase
        double[] times = new double[MEASUREMENT_ITERATIONS];
        for (int iter = 0; iter < MEASUREMENT_ITERATIONS; iter++) {
            world = new EcsWorld();
            List<Entity> entities = createEntitiesWithPosition(count);
            List<Velocity> velocities = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                velocities.add(new Velocity(1.0f, 1.0f, 1.0f));
            }

            long startTime = System.nanoTime();
            for (int i = 0; i < count; i++) {
                world.addComponent(entities.get(i), velocities.get(i));
            }
            long endTime = System.nanoTime();

            times[iter] = (endTime - startTime) / 1_000_000.0;
            System.out.println("  Iteration " + (iter + 1) + ": " + times[iter] + " ms");
        }
        
        double avg = average(times);
        double min = min(times);
        double max = max(times);
        System.out.println("  Result: avg=" + avg + " ms, min=" + min + " ms, max=" + max + " ms");
    }

    // ==================== Remove Component Performance Tests ====================

    @Test
    void performanceRemoveComponents1000() {
        measureRemoveComponentPerformance(1_000);
    }

    @Test
    void performanceRemoveComponents10000() {
        measureRemoveComponentPerformance(10_000);
    }

    @Test
    void performanceRemoveComponents100000() {
        measureRemoveComponentPerformance(100_000);
    }

    @Test
    void performanceRemoveComponents1000000() {
        measureRemoveComponentPerformance(1_000_000);
    }

    private void measureRemoveComponentPerformance(int count) {
        System.out.println("\n=== Remove Component Performance Test (n=" + count + ") ===");
        
        // Warmup phase
        int warmupSize = Math.max(1, count / WARMUP_SIZE_FACTOR);
        performWarmup(() -> {
            List<Entity> entities = createEntitiesWithPositionAndVelocity(warmupSize);
            for (Entity entity : entities) {
                world.removeComponent(entity, Velocity.class);
            }
        });
        
        // Measurement phase
        double[] times = new double[MEASUREMENT_ITERATIONS];
        for (int iter = 0; iter < MEASUREMENT_ITERATIONS; iter++) {
            world = new EcsWorld();
            List<Entity> entities = createEntitiesWithPositionAndVelocity(count);

            long startTime = System.nanoTime();
            for (Entity entity : entities) {
                world.removeComponent(entity, Velocity.class);
            }
            long endTime = System.nanoTime();

            times[iter] = (endTime - startTime) / 1_000_000.0;
            System.out.println("  Iteration " + (iter + 1) + ": " + times[iter] + " ms");
        }
        
        double avg = average(times);
        double min = min(times);
        double max = max(times);
        System.out.println("  Result: avg=" + avg + " ms, min=" + min + " ms, max=" + max + " ms");
    }

    // ==================== Query Performance Tests ====================

    @Test
    void performanceQueryComponents1000() {
        measureQueryPerformance(1_000);
    }

    @Test
    void performanceQueryComponents10000() {
        measureQueryPerformance(10_000);
    }

    @Test
    void performanceQueryComponents100000() {
        measureQueryPerformance(100_000);
    }

    @Test
    void performanceQueryComponents1000000() {
        measureQueryPerformance(1_000_000);
    }

    private void measureQueryPerformance(int count) {
        System.out.println("\n=== Query Performance Test (n=" + count + ") ===");
        
        // Warmup phase - create entities and query multiple times
        int warmupSize = Math.max(1, count / WARMUP_SIZE_FACTOR);
        performWarmup(() -> {
            createMixedEntities(warmupSize);
            for (int i = 0; i < WARMUP_QUERY_ITERATIONS; i++) {
                world.query(Position.class, Velocity.class);
            }
        });
        
        // Setup - create entities once for measurement
        world = new EcsWorld();
        createMixedEntities(count);
        
        // Measurement phase
        double[] times = new double[MEASUREMENT_ITERATIONS];
        int resultSize = 0;
        for (int iter = 0; iter < MEASUREMENT_ITERATIONS; iter++) {
            long startTime = System.nanoTime();
            List<Entity> result = world.query(Position.class, Velocity.class);
            long endTime = System.nanoTime();
            
            resultSize = result.size();
            times[iter] = (endTime - startTime) / 1_000_000.0;
            System.out.println("  Iteration " + (iter + 1) + ": " + times[iter] + " ms");
        }
        
        double avg = average(times);
        double min = min(times);
        double max = max(times);
        System.out.println("  Result: avg=" + avg + " ms, min=" + min + " ms, max=" + max + " ms (found " + resultSize + " matches)");
    }

    // ==================== Helper Methods ====================

    /**
     * Creates entities with only a Position component.
     */
    private List<Entity> createEntitiesWithPosition(int count) {
        List<Entity> entities = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Entity entity = world.createEntity(new Position(i, i, i));
            entities.add(entity);
        }
        return entities;
    }

    /**
     * Creates entities with Position and Velocity components.
     */
    private List<Entity> createEntitiesWithPositionAndVelocity(int count) {
        List<Entity> entities = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Entity entity = world.createEntity(
                    new Position(i, i, i),
                    new Velocity(1.0f, 1.0f, 1.0f)
            );
            entities.add(entity);
        }
        return entities;
    }

    /**
     * Creates a mix of entities with different component combinations:
     * - 1/3 with Position only
     * - 1/3 with Position and Velocity
     * - 1/3 with Position, Velocity, and Health
     */
    private void createMixedEntities(int count) {
        for (int i = 0; i < count; i++) {
            int type = i % 3;
            switch (type) {
                case 0 -> world.createEntity(new Position(i, i, i));
                case 1 -> world.createEntity(new Position(i, i, i), new Velocity(1.0f, 1.0f, 1.0f));
                case 2 -> world.createEntity(
                        new Position(i, i, i),
                        new Velocity(1.0f, 1.0f, 1.0f),
                        new Health(100, 100)
                );
            }
        }
    }
    
    // ==================== Statistics Helper Methods ====================
    
    private double average(double[] values) {
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }
    
    private double min(double[] values) {
        double min = values[0];
        for (double v : values) {
            if (v < min) min = v;
        }
        return min;
    }
    
    private double max(double[] values) {
        double max = values[0];
        for (double v : values) {
            if (v > max) max = v;
        }
        return max;
    }
}
