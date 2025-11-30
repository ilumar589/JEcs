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
 */
class PerformanceTest {

    private EcsWorld world;

    @BeforeEach
    void setUp() {
        world = new EcsWorld();
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

        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        System.out.println("Adding " + count + " components took " + elapsedMs + " ms");
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
        List<Entity> entities = createEntitiesWithPositionAndVelocity(count);

        long startTime = System.nanoTime();
        for (Entity entity : entities) {
            world.removeComponent(entity, Velocity.class);
        }
        long endTime = System.nanoTime();

        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        System.out.println("Removing " + count + " components took " + elapsedMs + " ms");
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
        createMixedEntities(count);

        long startTime = System.nanoTime();
        List<Entity> result = world.query(Position.class, Velocity.class);
        long endTime = System.nanoTime();

        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        System.out.println("Querying " + count + " entities took " + elapsedMs + " ms (found " + result.size() + " matches)");
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
}
