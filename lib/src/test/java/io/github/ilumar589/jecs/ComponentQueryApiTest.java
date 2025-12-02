package io.github.ilumar589.jecs;

import io.github.ilumar589.jecs.component.*;
import io.github.ilumar589.jecs.query.*;
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

    // ==================== Access Control Tests ====================

    @Test
    void forEachMutableUpdatesComponent() {
        world.spawn(new Position(1, 0, 0));
        world.spawn(new Position(2, 0, 0));

        // Use mutable access to update positions
        world.componentQuery()
            .withMutable(Position.class)
            .forEach(wrappers -> {
                @SuppressWarnings("unchecked")
                Mutable<Position> pos = (Mutable<Position>) wrappers[0];
                pos.update(p -> new Position(p.x() + 10, p.y(), p.z()));
            }, Position.class);

        // Verify positions were updated
        List<Float> xValues = new ArrayList<>();
        world.componentQuery()
            .forEach(Position.class, pos -> xValues.add(pos.x()));

        assertTrue(xValues.contains(11.0f));
        assertTrue(xValues.contains(12.0f));
    }

    @Test
    void forEachWithAccessReadOnlyAndMutable() {
        world.spawn(new Position(0, 0, 0), new Velocity(1, 2, 3));
        world.spawn(new Position(10, 0, 0), new Velocity(5, 0, 0));

        // Update position based on velocity (position is mutable, velocity is read-only)
        world.componentQuery()
            .withMutable(Position.class)
            .withReadOnly(Velocity.class)
            .forEach(wrappers -> {
                @SuppressWarnings("unchecked")
                Mutable<Position> pos = (Mutable<Position>) wrappers[0];
                @SuppressWarnings("unchecked")
                ReadOnly<Velocity> vel = (ReadOnly<Velocity>) wrappers[1];
                pos.update(p -> new Position(
                    p.x() + vel.get().dx(),
                    p.y() + vel.get().dy(),
                    p.z() + vel.get().dz()
                ));
            }, Position.class, Velocity.class);

        // Verify positions were updated based on velocities
        List<Float> xValues = new ArrayList<>();
        world.componentQuery()
            .forEach(Position.class, pos -> xValues.add(pos.x()));

        assertTrue(xValues.contains(1.0f));  // 0 + 1
        assertTrue(xValues.contains(15.0f)); // 10 + 5
    }

    @Test
    void readOnlyAndMutableMarking() {
        var query = world.componentQuery()
            .withReadOnly(Position.class)
            .withMutable(Velocity.class);

        assertTrue(query.isReadOnly(Position.class));
        assertFalse(query.isMutable(Position.class));
        assertTrue(query.isMutable(Velocity.class));
        assertFalse(query.isReadOnly(Velocity.class));
    }

    @Test
    void mutableWrapperTracksModifications() {
        world.spawn(new Position(1, 0, 0));
        world.spawn(new Position(2, 0, 0));

        // Only modify positions with x > 1
        world.componentQuery()
            .withMutable(Position.class)
            .forEach(wrappers -> {
                @SuppressWarnings("unchecked")
                Mutable<Position> pos = (Mutable<Position>) wrappers[0];
                if (pos.get().x() > 1.5f) {
                    pos.set(new Position(100, 0, 0));
                }
                // Don't modify positions with x <= 1.5
            }, Position.class);

        List<Float> xValues = new ArrayList<>();
        world.componentQuery()
            .forEach(Position.class, pos -> xValues.add(pos.x()));

        assertTrue(xValues.contains(1.0f));   // Unchanged
        assertTrue(xValues.contains(100.0f)); // Changed
    }

    @Test
    void physicsSystemWithAccessControl() {
        // Setup: Create moving entities
        for (int i = 0; i < 50; i++) {
            world.spawn(
                new Position(i, 0, 0),
                new Velocity(1, 0, 0)
            );
        }

        // Physics system: Update positions based on velocity
        // Position is mutable, Velocity is read-only
        world.componentQuery()
            .withMutable(Position.class)
            .withReadOnly(Velocity.class)
            .forEach(wrappers -> {
                @SuppressWarnings("unchecked")
                Mutable<Position> pos = (Mutable<Position>) wrappers[0];
                @SuppressWarnings("unchecked")
                ReadOnly<Velocity> vel = (ReadOnly<Velocity>) wrappers[1];
                Position current = pos.get();
                Velocity velocity = vel.get();
                pos.set(new Position(
                    current.x() + velocity.dx(),
                    current.y() + velocity.dy(),
                    current.z() + velocity.dz()
                ));
            }, Position.class, Velocity.class);

        // Verify all positions were updated
        AtomicInteger updated = new AtomicInteger(0);
        world.componentQuery()
            .forEach(Position.class, pos -> {
                if (pos.x() >= 1.0f) {
                    updated.incrementAndGet();
                }
            });

        assertEquals(50, updated.get());
    }

    @Test
    void healthSystemWithAccessControlAndExclusion() {
        // Create living and dead entities
        world.spawn(new Health(100, 100));
        world.spawn(new Health(50, 100));
        world.spawn(new Health(0, 100), new Dead());

        // Damage only living entities
        world.componentQuery()
            .withMutable(Health.class)
            .without(Dead.class)
            .forEach(wrappers -> {
                @SuppressWarnings("unchecked")
                Mutable<Health> health = (Mutable<Health>) wrappers[0];
                health.update(h -> new Health(
                    Math.max(0, h.current() - 10),
                    h.max()
                ));
            }, Health.class);

        // Verify only living entities were damaged
        List<Integer> healthValues = new ArrayList<>();
        world.componentQuery()
            .without(Dead.class)
            .forEach(Health.class, h -> healthValues.add(h.current()));

        assertTrue(healthValues.contains(90));  // 100 - 10
        assertTrue(healthValues.contains(40));  // 50 - 10
        assertEquals(2, healthValues.size());

        // Verify dead entity unchanged
        AtomicInteger deadHealth = new AtomicInteger(-1);
        world.componentQuery()
            .with(Dead.class)
            .forEach(Health.class, h -> deadHealth.set(h.current()));
        assertEquals(0, deadHealth.get());
    }

    // ==================== New Generic forEach Tests ====================

    @Test
    void genericForEachWithFiveComponents() {
        // Create entities with 5 components
        world.spawn(
            new Position(1, 2, 3),
            new Velocity(0.5f, 0.5f, 0.5f),
            new Health(100, 100),
            new Weapon(50),
            new Enemy(5)
        );

        AtomicInteger count = new AtomicInteger(0);
        world.componentQuery()
            .withMutable(Position.class, Health.class)
            .withReadOnly(Velocity.class, Weapon.class, Enemy.class)
            .forEach(wrappers -> {
                @SuppressWarnings("unchecked")
                Mutable<Position> pos = (Mutable<Position>) wrappers[0];
                @SuppressWarnings("unchecked")
                Mutable<Health> health = (Mutable<Health>) wrappers[1];
                @SuppressWarnings("unchecked")
                ReadOnly<Velocity> vel = (ReadOnly<Velocity>) wrappers[2];
                @SuppressWarnings("unchecked")
                ReadOnly<Weapon> weapon = (ReadOnly<Weapon>) wrappers[3];
                @SuppressWarnings("unchecked")
                ReadOnly<Enemy> enemy = (ReadOnly<Enemy>) wrappers[4];

                // Verify we can read all components
                assertEquals(1.0f, pos.get().x());
                assertEquals(0.5f, vel.get().dx());
                assertEquals(100, health.get().current());
                assertEquals(50, weapon.get().damage());
                assertEquals(5, enemy.get().level());

                // Update mutable components
                pos.set(new Position(10, 20, 30));
                health.set(new Health(50, 100));

                count.incrementAndGet();
            }, Position.class, Health.class, Velocity.class, Weapon.class, Enemy.class);

        assertEquals(1, count.get());

        // Verify updates were applied
        world.componentQuery()
            .forEach(Position.class, pos -> {
                assertEquals(10.0f, pos.x());
                assertEquals(20.0f, pos.y());
                assertEquals(30.0f, pos.z());
            });

        world.componentQuery()
            .forEach(Health.class, h -> {
                assertEquals(50, h.current());
                assertEquals(100, h.max());
            });
    }

    @Test
    void readOnlyThrowsExceptionOnSet() {
        world.spawn(new Position(1, 2, 3));

        world.componentQuery()
            .withReadOnly(Position.class)
            .forEach(wrappers -> {
                @SuppressWarnings("unchecked")
                ReadOnly<Position> pos = (ReadOnly<Position>) wrappers[0];

                // Verify we can read
                assertEquals(1.0f, pos.get().x());

                // Verify set throws exception
                assertThrows(UnsupportedOperationException.class, () -> {
                    pos.set(new Position(100, 200, 300));
                });
            }, Position.class);
    }

    @Test
    void readOnlyThrowsExceptionOnUpdate() {
        world.spawn(new Velocity(1, 2, 3));

        world.componentQuery()
            .withReadOnly(Velocity.class)
            .forEach(wrappers -> {
                @SuppressWarnings("unchecked")
                ReadOnly<Velocity> vel = (ReadOnly<Velocity>) wrappers[0];

                // Verify we can read
                assertEquals(1.0f, vel.get().dx());

                // Verify update throws exception
                assertThrows(UnsupportedOperationException.class, () -> {
                    vel.update(v -> new Velocity(v.dx() + 10, v.dy(), v.dz()));
                });
            }, Velocity.class);
    }

    @Test
    void directPrimitiveWritesAreImmediatelyVisible() {
        world.spawn(new Position(0, 0, 0));

        // Create a mutable wrapper and verify writes are visible immediately
        world.componentQuery()
            .withMutable(Position.class)
            .forEach(wrappers -> {
                @SuppressWarnings("unchecked")
                Mutable<Position> pos = (Mutable<Position>) wrappers[0];

                // Initial value
                assertEquals(0.0f, pos.get().x());

                // Write a new value
                pos.set(new Position(100, 200, 300));

                // Value should be immediately visible through get()
                assertEquals(100.0f, pos.get().x());
                assertEquals(200.0f, pos.get().y());
                assertEquals(300.0f, pos.get().z());
            }, Position.class);

        // Verify the write persisted after the forEach
        world.componentQuery()
            .forEach(Position.class, pos -> {
                assertEquals(100.0f, pos.x());
                assertEquals(200.0f, pos.y());
                assertEquals(300.0f, pos.z());
            });
    }

    @Test
    void mixedMutableAndReadOnlyComponents() {
        world.spawn(new Position(0, 0, 0), new Velocity(1, 2, 3), new Health(100, 100));

        world.componentQuery()
            .withMutable(Position.class)
            .withReadOnly(Velocity.class)
            .withMutable(Health.class)
            .forEach(wrappers -> {
                @SuppressWarnings("unchecked")
                Mutable<Position> pos = (Mutable<Position>) wrappers[0];
                @SuppressWarnings("unchecked")
                ReadOnly<Velocity> vel = (ReadOnly<Velocity>) wrappers[1];
                @SuppressWarnings("unchecked")
                Mutable<Health> health = (Mutable<Health>) wrappers[2];

                // Read from read-only
                float dx = vel.get().dx();
                float dy = vel.get().dy();
                float dz = vel.get().dz();

                // Write to mutable components based on read-only
                pos.update(p -> new Position(p.x() + dx, p.y() + dy, p.z() + dz));
                health.update(h -> new Health(h.current() - 10, h.max()));
            }, Position.class, Velocity.class, Health.class);

        // Verify updates
        world.componentQuery()
            .forEach(Position.class, pos -> {
                assertEquals(1.0f, pos.x());
                assertEquals(2.0f, pos.y());
                assertEquals(3.0f, pos.z());
            });

        world.componentQuery()
            .forEach(Health.class, h -> {
                assertEquals(90, h.current());
            });

        // Verify velocity unchanged
        world.componentQuery()
            .forEach(Velocity.class, vel -> {
                assertEquals(1.0f, vel.dx());
                assertEquals(2.0f, vel.dy());
                assertEquals(3.0f, vel.dz());
            });
    }

    @Test
    void componentWrapperInterfaceWorks() {
        world.spawn(new Position(1, 2, 3), new Velocity(4, 5, 6));

        world.componentQuery()
            .withMutable(Position.class)
            .withReadOnly(Velocity.class)
            .forEach(wrappers -> {
                // Both Mutable and ReadOnly implement ComponentWrapper
                ComponentWrapper<Position> posWrapper = (ComponentWrapper<Position>) wrappers[0];
                ComponentWrapper<Velocity> velWrapper = (ComponentWrapper<Velocity>) wrappers[1];

                // Can access via the interface
                Position pos = posWrapper.get();
                Velocity vel = velWrapper.get();

                assertEquals(1.0f, pos.x());
                assertEquals(4.0f, vel.dx());
            }, Position.class, Velocity.class);
    }

    @Test
    void multipleEntitiesWithGenericForEach() {
        // Create 10 entities
        for (int i = 0; i < 10; i++) {
            world.spawn(new Position(i, 0, 0), new Velocity(1, 0, 0));
        }

        AtomicInteger count = new AtomicInteger(0);
        world.componentQuery()
            .withMutable(Position.class)
            .withReadOnly(Velocity.class)
            .forEach(wrappers -> {
                @SuppressWarnings("unchecked")
                Mutable<Position> pos = (Mutable<Position>) wrappers[0];
                @SuppressWarnings("unchecked")
                ReadOnly<Velocity> vel = (ReadOnly<Velocity>) wrappers[1];

                // Update position based on velocity
                pos.update(p -> new Position(
                    p.x() + vel.get().dx(),
                    p.y() + vel.get().dy(),
                    p.z() + vel.get().dz()
                ));

                count.incrementAndGet();
            }, Position.class, Velocity.class);

        assertEquals(10, count.get());

        // Verify all positions were updated
        List<Float> xValues = new ArrayList<>();
        world.componentQuery()
            .forEach(Position.class, pos -> xValues.add(pos.x()));

        // Each x value should be increased by 1
        for (int i = 0; i < 10; i++) {
            assertTrue(xValues.contains((float)(i + 1)));
        }
    }

    @Test
    void readOnlyExceptionMessageIsHelpful() {
        world.spawn(new Position(1, 2, 3));

        world.componentQuery()
            .withReadOnly(Position.class)
            .forEach(wrappers -> {
                @SuppressWarnings("unchecked")
                ReadOnly<Position> pos = (ReadOnly<Position>) wrappers[0];

                // Read to cache the value
                pos.get();

                // Verify exception message is helpful
                UnsupportedOperationException exception = assertThrows(
                    UnsupportedOperationException.class,
                    () -> pos.set(new Position(100, 200, 300))
                );

                assertTrue(exception.getMessage().contains("Position"));
                assertTrue(exception.getMessage().contains("read-only"));
                assertTrue(exception.getMessage().contains("withMutable"));
            }, Position.class);
    }

    // ==================== Type-Safe forEachWrapper Tests ====================

    @Test
    void typeSafeForEachWrapperWith1Component() {
        world.spawn(new Position(1, 2, 3));
        world.spawn(new Position(10, 20, 30));

        AtomicInteger count = new AtomicInteger(0);
        List<Float> xValues = new ArrayList<>();

        world.componentQuery()
            .withMutable(Position.class)
            .forEachWrapper(Position.class, pos -> {
                // No manual cast needed - pos is already ComponentWrapper<Position>
                xValues.add(pos.get().x());
                // Can cast to Mutable since declared with withMutable
                Mutable<Position> mutablePos = (Mutable<Position>) pos;
                mutablePos.update(p -> new Position(p.x() + 100, p.y(), p.z()));
                count.incrementAndGet();
            });

        assertEquals(2, count.get());
        assertTrue(xValues.contains(1.0f));
        assertTrue(xValues.contains(10.0f));

        // Verify updates were applied
        List<Float> updatedXValues = new ArrayList<>();
        world.componentQuery()
            .forEach(Position.class, pos -> updatedXValues.add(pos.x()));

        assertTrue(updatedXValues.contains(101.0f));
        assertTrue(updatedXValues.contains(110.0f));
    }

    @Test
    void typeSafeForEachWrapperWith2Components() {
        world.spawn(new Position(1, 0, 0), new Velocity(10, 0, 0));
        world.spawn(new Position(2, 0, 0), new Velocity(20, 0, 0));

        AtomicInteger count = new AtomicInteger(0);

        world.componentQuery()
            .withMutable(Position.class)
            .withReadOnly(Velocity.class)
            .forEachWrapper(Position.class, Velocity.class, (pos, vel) -> {
                // Both wrappers are typed - no manual casts needed for get()!
                float dx = vel.get().dx();
                
                // Cast to Mutable only for modification operations
                Mutable<Position> mutablePos = (Mutable<Position>) pos;
                mutablePos.update(p -> new Position(p.x() + dx, p.y(), p.z()));
                count.incrementAndGet();
            });

        assertEquals(2, count.get());

        // Verify positions updated based on velocity
        List<Float> xValues = new ArrayList<>();
        world.componentQuery()
            .forEach(Position.class, pos -> xValues.add(pos.x()));

        assertTrue(xValues.contains(11.0f));  // 1 + 10
        assertTrue(xValues.contains(22.0f));  // 2 + 20
    }

    @Test
    void typeSafeForEachWrapperWith3Components() {
        world.spawn(new Position(1, 0, 0), new Velocity(5, 0, 0), new Health(100, 100));

        AtomicInteger count = new AtomicInteger(0);

        world.componentQuery()
            .withMutable(Position.class, Health.class)
            .withReadOnly(Velocity.class)
            .forEachWrapper(Position.class, Velocity.class, Health.class, (pos, vel, health) -> {
                // Access all typed wrappers
                assertEquals(1.0f, pos.get().x());
                assertEquals(5.0f, vel.get().dx());
                assertEquals(100, health.get().current());

                // Update mutable components
                ((Mutable<Position>) pos).update(p -> new Position(p.x() + vel.get().dx(), p.y(), p.z()));
                ((Mutable<Health>) health).update(h -> new Health(h.current() - 10, h.max()));

                count.incrementAndGet();
            });

        assertEquals(1, count.get());

        // Verify updates
        world.componentQuery()
            .forEach(Position.class, pos -> assertEquals(6.0f, pos.x()));
        world.componentQuery()
            .forEach(Health.class, h -> assertEquals(90, h.current()));
    }

    @Test
    void typeSafeForEachWrapperWith4Components() {
        world.spawn(new Position(1, 0, 0), new Velocity(10, 0, 0), 
                    new Health(100, 100), new Weapon(50));

        AtomicInteger count = new AtomicInteger(0);

        world.componentQuery()
            .withMutable(Position.class)
            .withReadOnly(Velocity.class, Health.class, Weapon.class)
            .forEachWrapper(Position.class, Velocity.class, Health.class, Weapon.class,
                (pos, vel, health, weapon) -> {
                    // All 4 wrappers are typed
                    assertEquals(1.0f, pos.get().x());
                    assertEquals(10.0f, vel.get().dx());
                    assertEquals(100, health.get().current());
                    assertEquals(50, weapon.get().damage());

                    // Only Position is mutable
                    ((Mutable<Position>) pos).set(new Position(999, 0, 0));

                    count.incrementAndGet();
                });

        assertEquals(1, count.get());

        world.componentQuery()
            .forEach(Position.class, pos -> assertEquals(999.0f, pos.x()));
    }

    @Test
    void typeSafeForEachWrapperWith5Components() {
        world.spawn(new Position(1, 0, 0), new Velocity(1, 0, 0), 
                    new Health(100, 100), new Weapon(50), new Enemy(5));

        AtomicInteger count = new AtomicInteger(0);

        world.componentQuery()
            .withMutable(Position.class, Health.class)
            .withReadOnly(Velocity.class, Weapon.class, Enemy.class)
            .forEachWrapper(Position.class, Velocity.class, Health.class, Weapon.class, Enemy.class,
                (pos, vel, health, weapon, enemy) -> {
                    // All 5 wrappers are typed
                    assertEquals(1.0f, pos.get().x());
                    assertEquals(1.0f, vel.get().dx());
                    assertEquals(100, health.get().current());
                    assertEquals(50, weapon.get().damage());
                    assertEquals(5, enemy.get().level());

                    // Update mutable components
                    ((Mutable<Position>) pos).update(p -> new Position(
                        p.x() + vel.get().dx() * weapon.get().damage(), p.y(), p.z()));
                    ((Mutable<Health>) health).update(h -> new Health(
                        h.current() - enemy.get().level(), h.max()));

                    count.incrementAndGet();
                });

        assertEquals(1, count.get());

        // Verify: position = 1 + 1*50 = 51, health = 100 - 5 = 95
        world.componentQuery()
            .forEach(Position.class, pos -> assertEquals(51.0f, pos.x()));
        world.componentQuery()
            .forEach(Health.class, h -> assertEquals(95, h.current()));
    }

    @Test
    void typeSafeForEachWrapperWith6Components() {
        // Need a 6th component - reuse Dead as a marker
        world.spawn(new Position(1, 0, 0), new Velocity(2, 0, 0), 
                    new Health(100, 100), new Weapon(10), new Enemy(3), new Dead());

        AtomicInteger count = new AtomicInteger(0);

        world.componentQuery()
            .withMutable(Position.class)
            .withReadOnly(Velocity.class, Health.class, Weapon.class, Enemy.class, Dead.class)
            .forEachWrapper(Position.class, Velocity.class, Health.class, 
                           Weapon.class, Enemy.class, Dead.class,
                (pos, vel, health, weapon, enemy, dead) -> {
                    // All 6 wrappers are typed!
                    assertEquals(1.0f, pos.get().x());
                    assertEquals(2.0f, vel.get().dx());
                    assertEquals(100, health.get().current());
                    assertEquals(10, weapon.get().damage());
                    assertEquals(3, enemy.get().level());
                    assertNotNull(dead.get()); // Dead is just a marker

                    ((Mutable<Position>) pos).set(new Position(6, 6, 6));

                    count.incrementAndGet();
                });

        assertEquals(1, count.get());

        world.componentQuery()
            .forEach(Position.class, pos -> {
                assertEquals(6.0f, pos.x());
                assertEquals(6.0f, pos.y());
                assertEquals(6.0f, pos.z());
            });
    }

    @Test
    void typeSafeForEachWrapperWithReadOnlyPreventsModification() {
        world.spawn(new Position(1, 2, 3));

        world.componentQuery()
            .withReadOnly(Position.class)
            .forEachWrapper(Position.class, pos -> {
                // Can read without issues
                assertEquals(1.0f, pos.get().x());

                // Casting to ReadOnly and trying to modify should throw
                ReadOnly<Position> readOnlyPos = (ReadOnly<Position>) pos;
                assertThrows(UnsupportedOperationException.class, () -> {
                    readOnlyPos.set(new Position(100, 200, 300));
                });
            });
    }

    @Test
    void typeSafeForEachWrapperMultipleEntities() {
        // Create 20 entities with 2 components each
        for (int i = 0; i < 20; i++) {
            world.spawn(new Position(i, 0, 0), new Velocity(1, 0, 0));
        }

        AtomicInteger count = new AtomicInteger(0);

        world.componentQuery()
            .withMutable(Position.class)
            .withReadOnly(Velocity.class)
            .forEachWrapper(Position.class, Velocity.class, (pos, vel) -> {
                ((Mutable<Position>) pos).update(p -> new Position(
                    p.x() + vel.get().dx(), p.y(), p.z()));
                count.incrementAndGet();
            });

        assertEquals(20, count.get());

        // Verify all positions were updated (each x increased by 1)
        List<Float> xValues = new ArrayList<>();
        world.componentQuery()
            .forEach(Position.class, pos -> xValues.add(pos.x()));

        assertEquals(20, xValues.size());
        for (int i = 0; i < 20; i++) {
            assertTrue(xValues.contains((float)(i + 1)), "Should contain position with x=" + (i + 1));
        }
    }

    @Test
    void typeSafeForEachWrapperWithExclusion() {
        world.spawn(new Position(1, 0, 0), new Velocity(5, 0, 0));
        world.spawn(new Position(2, 0, 0), new Velocity(10, 0, 0), new Dead()); // Should be excluded

        AtomicInteger count = new AtomicInteger(0);

        world.componentQuery()
            .withMutable(Position.class)
            .withReadOnly(Velocity.class)
            .without(Dead.class)
            .forEachWrapper(Position.class, Velocity.class, (pos, vel) -> {
                ((Mutable<Position>) pos).update(p -> new Position(p.x() + vel.get().dx(), p.y(), p.z()));
                count.incrementAndGet();
            });

        assertEquals(1, count.get());

        // Only the living entity should be updated
        List<Float> xValues = new ArrayList<>();
        world.componentQuery()
            .without(Dead.class)
            .forEach(Position.class, pos -> xValues.add(pos.x()));

        assertEquals(1, xValues.size());
        assertTrue(xValues.contains(6.0f));  // 1 + 5
    }

    @Test
    void typeSafeForEachWrapperMaintainsWrapperType() {
        world.spawn(new Position(1, 0, 0), new Velocity(2, 0, 0));

        world.componentQuery()
            .withMutable(Position.class)
            .withReadOnly(Velocity.class)
            .forEachWrapper(Position.class, Velocity.class, (pos, vel) -> {
                // Verify wrapper types are correct
                assertTrue(pos instanceof Mutable);
                assertTrue(vel instanceof ReadOnly);
            });
    }
}
