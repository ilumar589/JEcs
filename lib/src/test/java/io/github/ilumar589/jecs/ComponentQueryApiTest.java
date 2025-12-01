package io.github.ilumar589.jecs;

import io.github.ilumar589.jecs.component.*;
import io.github.ilumar589.jecs.query.ComponentTuple2;
import io.github.ilumar589.jecs.query.ComponentTuple3;
import io.github.ilumar589.jecs.query.ComponentTuple4;
import io.github.ilumar589.jecs.world.EcsWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the component-focused API.
 * These tests demonstrate the new fluent query API and ensure
 * that users never need to work directly with Entity or Archetype classes.
 */
class ComponentQueryApiTest {

    private EcsWorld world;

    @BeforeEach
    void setUp() {
        world = new EcsWorld();
    }

    // ==================== Basic Spawning Tests ====================

    @Test
    void spawnEntityWithSingleComponent() {
        world.spawn(new Position(1, 2, 3));
        assertEquals(1, world.getEntityCount());
    }

    @Test
    void spawnEntityWithMultipleComponents() {
        world.spawn(new Position(1, 2, 3), new Velocity(0.1f, 0.2f, 0.3f));
        assertEquals(1, world.getEntityCount());
    }

    @Test
    void spawnMultipleEntities() {
        world.spawn(new Position(1, 0, 0));
        world.spawn(new Position(2, 0, 0));
        world.spawn(new Position(3, 0, 0));
        assertEquals(3, world.getEntityCount());
    }

    @Test
    void spawnBatchCreatesCorrectNumberOfEntities() {
        world.spawnBatch(100, () -> new Position(0, 0, 0));
        assertEquals(100, world.getEntityCount());
    }

    @Test
    void spawnBatchWithMultipleComponentSuppliers() {
        Random random = new Random(42);
        world.spawnBatch(50,
            () -> new Position(random.nextFloat(), random.nextFloat(), random.nextFloat()),
            () -> new Velocity(1, 0, 0)
        );
        assertEquals(50, world.getEntityCount());
    }

    @Test
    void spawnBatchGeneratesUniqueComponents() {
        AtomicInteger counter = new AtomicInteger(0);
        world.spawnBatch(10, () -> new Position(counter.incrementAndGet(), 0, 0));

        List<Float> xValues = new ArrayList<>();
        world.componentQuery()
            .forEach(Position.class, pos -> xValues.add(pos.x()));

        // Verify all x values are unique (1 through 10)
        assertEquals(10, xValues.size());
        for (int i = 1; i <= 10; i++) {
            assertTrue(xValues.contains((float) i), "Should contain position with x=" + i);
        }
    }

    // ==================== Single Component Query Tests ====================

    @Test
    void queryWithSingleComponent() {
        world.spawn(new Position(1, 0, 0));
        world.spawn(new Position(2, 0, 0));
        world.spawn(new Velocity(1, 0, 0)); // No Position

        AtomicInteger count = new AtomicInteger(0);
        world.componentQuery()
            .forEach(Position.class, pos -> count.incrementAndGet());

        assertEquals(2, count.get());
    }

    @Test
    void queryWithSingleComponentReturnsCorrectValues() {
        world.spawn(new Position(1, 2, 3));

        world.componentQuery()
            .forEach(Position.class, pos -> {
                assertEquals(1.0f, pos.x());
                assertEquals(2.0f, pos.y());
                assertEquals(3.0f, pos.z());
            });
    }

    // ==================== Two Component Query Tests ====================

    @Test
    void queryWithTwoComponents() {
        world.spawn(new Position(1, 0, 0), new Velocity(1, 0, 0));
        world.spawn(new Position(2, 0, 0), new Velocity(2, 0, 0));
        world.spawn(new Position(3, 0, 0)); // No Velocity

        AtomicInteger count = new AtomicInteger(0);
        world.componentQuery()
            .forEach(Position.class, Velocity.class, (pos, vel) -> count.incrementAndGet());

        assertEquals(2, count.get());
    }

    @Test
    void queryWithTwoComponentsReturnsCorrectPairs() {
        world.spawn(new Position(10, 20, 30), new Velocity(1, 2, 3));

        world.componentQuery()
            .forEach(Position.class, Velocity.class, (pos, vel) -> {
                assertEquals(10.0f, pos.x());
                assertEquals(20.0f, pos.y());
                assertEquals(30.0f, pos.z());
                assertEquals(1.0f, vel.dx());
                assertEquals(2.0f, vel.dy());
                assertEquals(3.0f, vel.dz());
            });
    }

    // ==================== Three Component Query Tests ====================

    @Test
    void queryWithThreeComponents() {
        world.spawn(new Position(1, 0, 0), new Velocity(1, 0, 0), new Health(100, 100));
        world.spawn(new Position(2, 0, 0), new Velocity(2, 0, 0)); // No Health
        world.spawn(new Position(3, 0, 0), new Velocity(3, 0, 0), new Health(50, 100));

        AtomicInteger count = new AtomicInteger(0);
        world.componentQuery()
            .forEach(Position.class, Velocity.class, Health.class, 
                (pos, vel, health) -> count.incrementAndGet());

        assertEquals(2, count.get());
    }

    // ==================== Four Component Query Tests ====================

    @Test
    void queryWithFourComponents() {
        world.spawn(new Position(1, 0, 0), new Velocity(1, 0, 0), new Health(100, 100), new Weapon(10));
        world.spawn(new Position(2, 0, 0), new Velocity(2, 0, 0), new Health(50, 100)); // No Weapon

        AtomicInteger count = new AtomicInteger(0);
        world.componentQuery()
            .forEach(Position.class, Velocity.class, Health.class, Weapon.class,
                (pos, vel, health, weapon) -> count.incrementAndGet());

        assertEquals(1, count.get());
    }

    // ==================== Exclusion Query Tests (without) ====================

    @Test
    void queryWithExclusion() {
        world.spawn(new Position(1, 0, 0), new Health(100, 100));
        world.spawn(new Position(2, 0, 0), new Health(0, 100), new Dead());
        world.spawn(new Position(3, 0, 0), new Health(50, 100));

        AtomicInteger count = new AtomicInteger(0);
        world.componentQuery()
            .without(Dead.class)
            .forEach(Position.class, Health.class, (pos, health) -> count.incrementAndGet());

        assertEquals(2, count.get()); // Only the two living entities
    }

    @Test
    void queryWithMultipleExclusions() {
        world.spawn(new Position(1, 0, 0), new Health(100, 100));
        world.spawn(new Position(2, 0, 0), new Health(0, 100), new Dead());
        world.spawn(new Position(3, 0, 0), new Health(50, 100), new Velocity(1, 0, 0));

        AtomicInteger count = new AtomicInteger(0);
        world.componentQuery()
            .without(Dead.class, Velocity.class)
            .forEach(Position.class, Health.class, (pos, health) -> count.incrementAndGet());

        assertEquals(1, count.get()); // Only the first entity
    }

    @Test
    void queryExclusionDoesNotAffectEntitiesWithoutExcludedComponent() {
        world.spawn(new Position(1, 0, 0));
        world.spawn(new Position(2, 0, 0));
        world.spawn(new Position(3, 0, 0), new Dead());

        int count = world.componentQuery()
            .with(Position.class)
            .without(Dead.class)
            .count();

        assertEquals(2, count);
    }

    // ==================== Count Operation Tests ====================

    @Test
    void countReturnsCorrectNumber() {
        world.spawn(new Position(1, 0, 0));
        world.spawn(new Position(2, 0, 0));
        world.spawn(new Position(3, 0, 0));

        int count = world.componentQuery()
            .with(Position.class)
            .count();

        assertEquals(3, count);
    }

    @Test
    void countWithExclusionReturnsCorrectNumber() {
        world.spawn(new Position(1, 0, 0));
        world.spawn(new Position(2, 0, 0), new Dead());
        world.spawn(new Position(3, 0, 0));

        int count = world.componentQuery()
            .with(Position.class)
            .without(Dead.class)
            .count();

        assertEquals(2, count);
    }

    @Test
    void countReturnsZeroWhenNoMatches() {
        world.spawn(new Position(1, 0, 0));

        int count = world.componentQuery()
            .with(Velocity.class)
            .count();

        assertEquals(0, count);
    }

    // ==================== Any Operation Tests ====================

    @Test
    void anyReturnsTrueWhenMatchesExist() {
        world.spawn(new Position(1, 0, 0));

        boolean hasPosition = world.componentQuery()
            .with(Position.class)
            .any();

        assertTrue(hasPosition);
    }

    @Test
    void anyReturnsFalseWhenNoMatches() {
        world.spawn(new Position(1, 0, 0));

        boolean hasVelocity = world.componentQuery()
            .with(Velocity.class)
            .any();

        assertFalse(hasVelocity);
    }

    @Test
    void anyWithExclusionWorksCorrectly() {
        world.spawn(new Position(1, 0, 0), new Dead());

        boolean hasLivingPosition = world.componentQuery()
            .with(Position.class)
            .without(Dead.class)
            .any();

        assertFalse(hasLivingPosition);
    }

    // ==================== Results (Component Tuple) Tests ====================

    @Test
    void results2ReturnsCorrectTuples() {
        world.spawn(new Position(1, 0, 0), new Velocity(0.1f, 0, 0));
        world.spawn(new Position(2, 0, 0), new Velocity(0.2f, 0, 0));

        List<ComponentTuple2<Position, Velocity>> results = world.componentQuery()
            .results2(Position.class, Velocity.class);

        assertEquals(2, results.size());
        
        // Verify tuple contents (order may vary based on internal storage)
        boolean found1 = false, found2 = false;
        for (var tuple : results) {
            if (tuple.first().x() == 1.0f && tuple.second().dx() == 0.1f) found1 = true;
            if (tuple.first().x() == 2.0f && tuple.second().dx() == 0.2f) found2 = true;
        }
        assertTrue(found1 && found2, "Should find both entities in results");
    }

    @Test
    void results3ReturnsCorrectTuples() {
        world.spawn(new Position(1, 0, 0), new Velocity(1, 0, 0), new Health(100, 100));

        List<ComponentTuple3<Position, Velocity, Health>> results = world.componentQuery()
            .results3(Position.class, Velocity.class, Health.class);

        assertEquals(1, results.size());
        assertEquals(1.0f, results.getFirst().first().x());
        assertEquals(1.0f, results.getFirst().second().dx());
        assertEquals(100, results.getFirst().third().current());
    }

    @Test
    void results4ReturnsCorrectTuples() {
        world.spawn(new Position(1, 0, 0), new Velocity(1, 0, 0), new Health(100, 100), new Weapon(50));

        List<ComponentTuple4<Position, Velocity, Health, Weapon>> results = world.componentQuery()
            .results4(Position.class, Velocity.class, Health.class, Weapon.class);

        assertEquals(1, results.size());
        assertEquals(1.0f, results.getFirst().first().x());
        assertEquals(50, results.getFirst().fourth().damage());
    }

    // ==================== Modification Tests ====================

    @Test
    void modifyUpdatesComponent() {
        world.spawn(new Position(1, 0, 0));

        world.componentQuery()
            .modify(Position.class, pos -> new Position(pos.x() + 10, pos.y(), pos.z()));

        world.componentQuery()
            .forEach(Position.class, pos -> assertEquals(11.0f, pos.x()));
    }

    @Test
    void modifyIfOnlyModifiesMatchingComponents() {
        world.spawn(new Health(100, 100));
        world.spawn(new Health(50, 100));
        world.spawn(new Health(10, 100));

        // Only heal entities with health < 50
        world.componentQuery()
            .modifyIf(Health.class,
                health -> health.current() < 50,
                health -> new Health(health.current() + 20, health.max())
            );

        List<Integer> healthValues = new ArrayList<>();
        world.componentQuery()
            .forEach(Health.class, health -> healthValues.add(health.current()));

        assertTrue(healthValues.contains(100)); // Unchanged
        assertTrue(healthValues.contains(50));  // Unchanged
        assertTrue(healthValues.contains(30));  // 10 + 20
    }

    // ==================== Practical Example Tests ====================

    @Test
    void physicsSystemExample() {
        // Setup: Create moving entities
        for (int i = 0; i < 100; i++) {
            world.spawn(
                new Position(i, 0, 0),
                new Velocity(1, 0, 0)
            );
        }

        // Verify initial state
        int[] belowOne = {0};
        world.componentQuery()
            .forEach(Position.class, pos -> { if (pos.x() < 1) belowOne[0]++; });
        assertEquals(1, belowOne[0]); // Only position with x=0

        // System: Update positions based on velocity
        // Note: For this example, we verify the structure works
        List<Float> velocities = new ArrayList<>();
        world.componentQuery()
            .forEach(Position.class, Velocity.class, 
                (pos, vel) -> velocities.add(vel.dx()));

        assertEquals(100, velocities.size());
        
        // Verify entities exist
        assertEquals(100, world.componentQuery().with(Position.class).with(Velocity.class).count());
    }

    @Test
    void healthSystemWithDeathExample() {
        // Create living entities
        world.spawn(new Position(0, 0, 0), new Health(100, 100));
        world.spawn(new Position(1, 0, 0), new Health(50, 100));
        world.spawn(new Position(2, 0, 0), new Health(0, 100), new Dead());

        // Verify initial living count
        int livingCount = world.componentQuery()
            .with(Health.class)
            .without(Dead.class)
            .count();
        assertEquals(2, livingCount);

        // System: Apply damage to living entities only
        world.componentQuery()
            .without(Dead.class)
            .modify(Health.class, health -> new Health(
                Math.max(0, health.current() - 10),
                health.max()
            ));

        // Verify: Dead entities unaffected, living entities damaged
        List<Integer> livingHealthValues = new ArrayList<>();
        world.componentQuery()
            .without(Dead.class)
            .forEach(Health.class, health -> livingHealthValues.add(health.current()));

        assertTrue(livingHealthValues.contains(90));  // 100 - 10
        assertTrue(livingHealthValues.contains(40));  // 50 - 10

        // Verify dead entity still has 0 health
        AtomicInteger deadHealth = new AtomicInteger(-1);
        world.componentQuery()
            .forEach(Health.class, Dead.class, (health, dead) -> deadHealth.set(health.current()));
        assertEquals(0, deadHealth.get());
    }

    @Test
    void complexAIQueryExample() {
        // Create various entities
        world.spawn(new Enemy(1), new Weapon(10), new Health(100, 100), new Position(0, 0, 0));
        world.spawn(new Enemy(2), new Weapon(20), new Health(20, 100), new Position(10, 0, 0)); // Low health
        world.spawn(new Enemy(3), new Weapon(30), new Health(10, 100), new Position(20, 0, 0)); // Very low health
        world.spawn(new Enemy(4), new Weapon(40), new Health(0, 100), new Position(30, 0, 0), new Dead());

        // Find all armed, living enemies
        int armedLivingEnemies = world.componentQuery()
            .with(Enemy.class).with(Weapon.class).with(Health.class).with(Position.class)
            .without(Dead.class)
            .count();
        assertEquals(3, armedLivingEnemies);

        // Find enemies with low health (< 30%)
        List<Integer> lowHealthEnemyLevels = new ArrayList<>();
        world.componentQuery()
            .without(Dead.class)
            .forEach(Enemy.class, Weapon.class, Health.class, Position.class,
                (enemy, weapon, health, pos) -> {
                    if (health.current() < health.max() * 0.3) {
                        lowHealthEnemyLevels.add(enemy.level());
                    }
                });

        assertEquals(2, lowHealthEnemyLevels.size());
        assertTrue(lowHealthEnemyLevels.contains(2));
        assertTrue(lowHealthEnemyLevels.contains(3));
    }

    @Test
    void spawnerSystemExample() {
        Random random = new Random(12345);

        // Batch spawn enemies at random positions
        world.spawnBatch(50,
            () -> new Enemy(random.nextInt(10) + 1),
            () -> new Position(
                random.nextFloat() * 100,
                random.nextFloat() * 100,
                random.nextFloat() * 100
            ),
            () -> new Health(100, 100)
        );

        // Verify all enemies have position and health
        assertEquals(50, world.componentQuery()
            .with(Enemy.class).with(Position.class).with(Health.class)
            .count());

        // Verify different enemy levels were created
        List<Integer> levels = new ArrayList<>();
        world.componentQuery()
            .forEach(Enemy.class, enemy -> levels.add(enemy.level()));

        assertTrue(levels.stream().distinct().count() > 1,
            "Should have multiple different enemy levels");
    }

    // ==================== Empty Result Tests ====================

    @Test
    void queryOnEmptyWorldReturnsZeroCount() {
        assertEquals(0, world.componentQuery().with(Position.class).count());
    }

    @Test
    void queryOnEmptyWorldReturnsEmptyResults() {
        List<ComponentTuple2<Position, Velocity>> results = world.componentQuery()
            .results2(Position.class, Velocity.class);
        assertTrue(results.isEmpty());
    }

    @Test
    void queryWithNoMatchingEntitiesReturnsZeroCount() {
        world.spawn(new Position(1, 0, 0));

        assertEquals(0, world.componentQuery()
            .with(Position.class).with(Velocity.class)
            .count());
    }

    // ==================== Edge Case Tests ====================

    @Test
    void queryWithSameTypeMultipleTimes() {
        world.spawn(new Position(1, 0, 0));

        // Adding same type multiple times should still work
        int count = world.componentQuery()
            .with(Position.class)
            .with(Position.class)
            .count();

        assertEquals(1, count);
    }

    @Test
    void queryAllEntitiesMatching() {
        // All entities match the query
        world.spawn(new Position(1, 0, 0), new Velocity(1, 0, 0));
        world.spawn(new Position(2, 0, 0), new Velocity(2, 0, 0));
        world.spawn(new Position(3, 0, 0), new Velocity(3, 0, 0));

        int count = world.componentQuery()
            .with(Position.class).with(Velocity.class)
            .count();

        assertEquals(3, count);
    }

    @Test
    void queryWithExclusionWhenAllHaveExcludedComponent() {
        world.spawn(new Position(1, 0, 0), new Dead());
        world.spawn(new Position(2, 0, 0), new Dead());
        world.spawn(new Position(3, 0, 0), new Dead());

        int count = world.componentQuery()
            .with(Position.class)
            .without(Dead.class)
            .count();

        assertEquals(0, count);
    }

    @Test
    void multipleQueriesAreIndependent() {
        world.spawn(new Position(1, 0, 0), new Velocity(1, 0, 0));
        world.spawn(new Position(2, 0, 0));

        var query1 = world.componentQuery().with(Position.class);
        var query2 = world.componentQuery().with(Position.class).with(Velocity.class);

        // Queries should be independent
        assertEquals(2, query1.count());
        assertEquals(1, query2.count());
    }

    // ==================== Performance Baseline Tests ====================

    @Test
    void queryPerformanceWith10000Entities() {
        // Create mixed entities
        for (int i = 0; i < 10000; i++) {
            if (i % 3 == 0) {
                world.spawn(new Position(i, 0, 0), new Velocity(1, 0, 0));
            } else {
                world.spawn(new Position(i, 0, 0));
            }
        }

        // Query should complete quickly
        long startTime = System.nanoTime();
        int count = world.componentQuery()
            .with(Position.class).with(Velocity.class)
            .count();
        long endTime = System.nanoTime();

        assertEquals(3334, count); // ~1/3 of 10000
        assertTrue((endTime - startTime) / 1_000_000.0 < 100,
            "Query should complete in under 100ms");
    }

    @Test
    void queryWithExclusionPerformance() {
        // Create entities, half with Dead component
        for (int i = 0; i < 5000; i++) {
            if (i % 2 == 0) {
                world.spawn(new Position(i, 0, 0), new Health(100, 100));
            } else {
                world.spawn(new Position(i, 0, 0), new Health(0, 100), new Dead());
            }
        }

        long startTime = System.nanoTime();
        int count = world.componentQuery()
            .with(Position.class).with(Health.class)
            .without(Dead.class)
            .count();
        long endTime = System.nanoTime();

        assertEquals(2500, count);
        assertTrue((endTime - startTime) / 1_000_000.0 < 100,
            "Query with exclusion should complete in under 100ms");
    }

    @Test
    void batchSpawnPerformance() {
        long startTime = System.nanoTime();
        world.spawnBatch(10000,
            () -> new Position(0, 0, 0),
            () -> new Velocity(1, 0, 0)
        );
        long endTime = System.nanoTime();

        assertEquals(10000, world.getEntityCount());
        assertTrue((endTime - startTime) / 1_000_000.0 < 500,
            "Batch spawn of 10000 entities should complete in under 500ms");
    }
}
