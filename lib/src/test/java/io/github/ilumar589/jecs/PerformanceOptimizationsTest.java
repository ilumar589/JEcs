package io.github.ilumar589.jecs;

import io.github.ilumar589.jecs.component.*;
import io.github.ilumar589.jecs.query.*;
import io.github.ilumar589.jecs.world.ComponentTypeRegistry;
import io.github.ilumar589.jecs.world.EcsWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for performance optimizations in JEcs.
 * 
 * <h2>Tested Optimizations</h2>
 * <ul>
 *   <li>Wrapper reuse in forEach queries</li>
 *   <li>Query result caching</li>
 *   <li>Bitset-based component matching</li>
 *   <li>Reader/Writer caching in Archetype</li>
 * </ul>
 */
class PerformanceOptimizationsTest {

    private EcsWorld world;

    @BeforeEach
    void setUp() {
        world = new EcsWorld();
    }

    // ==================== Wrapper Reuse Tests ====================

    @Test
    void wrapperReuseAcrossEntities() {
        // Create multiple entities
        for (int i = 0; i < 100; i++) {
            world.spawn(new Position(i, 0, 0));
        }

        AtomicInteger count = new AtomicInteger(0);
        
        // Verify wrappers work correctly when reused
        world.componentQuery()
            .withMutable(Position.class)
            .forEach(wrappers -> {
                @SuppressWarnings("unchecked")
                Mutable<Position> pos = (Mutable<Position>) wrappers[0];
                
                // Verify we can read and write correctly
                float oldX = pos.get().x();
                pos.set(new Position(oldX + 1, 0, 0));
                
                // Verify write was applied
                assertEquals(oldX + 1, pos.get().x(), 0.01f);
                
                count.incrementAndGet();
            }, Position.class);

        assertEquals(100, count.get());
        
        // Verify all positions were updated
        AtomicInteger updatedCount = new AtomicInteger(0);
        world.componentQuery()
            .forEach(Position.class, pos -> {
                // Each position should have x >= 1 (was i, now i+1)
                assertTrue(pos.x() >= 1);
                updatedCount.incrementAndGet();
            });
        assertEquals(100, updatedCount.get());
    }

    @Test
    void wrapperCacheInvalidatedBetweenQueries() {
        world.spawn(new Position(1, 2, 3));
        world.spawn(new Position(10, 20, 30));

        // First query
        AtomicInteger sum1 = new AtomicInteger(0);
        world.componentQuery()
            .forEach(Position.class, pos -> {
                sum1.addAndGet((int) pos.x());
            });
        assertEquals(11, sum1.get());

        // Modify positions
        world.componentQuery()
            .modify(Position.class, pos -> new Position(pos.x() * 10, pos.y(), pos.z()));

        // Second query should see updated values
        AtomicInteger sum2 = new AtomicInteger(0);
        world.componentQuery()
            .forEach(Position.class, pos -> {
                sum2.addAndGet((int) pos.x());
            });
        assertEquals(110, sum2.get());
    }

    // ==================== Query Cache Tests ====================

    @Test
    void queryCacheHitOnRepeatedQueries() {
        // Create entities
        for (int i = 0; i < 100; i++) {
            world.spawn(new Position(i, 0, 0), new Velocity(1, 0, 0));
        }

        // First query - cache miss (populates cache)
        int count1 = world.componentQuery()
            .with(Position.class, Velocity.class)
            .count();

        // Second query - should hit cache
        int count2 = world.componentQuery()
            .with(Position.class, Velocity.class)
            .count();

        // Third query - should hit cache
        int count3 = world.componentQuery()
            .with(Position.class, Velocity.class)
            .count();

        assertEquals(100, count1);
        assertEquals(100, count2);
        assertEquals(100, count3);
    }

    @Test
    void queryCacheInvalidatedOnNewArchetype() {
        // Create initial entities
        for (int i = 0; i < 50; i++) {
            world.spawn(new Position(i, 0, 0), new Velocity(1, 0, 0));
        }

        // First query
        int count1 = world.componentQuery()
            .with(Position.class, Velocity.class)
            .count();
        assertEquals(50, count1);

        // Add entity with new archetype (Position + Velocity + Health)
        world.spawn(new Position(100, 0, 0), new Velocity(1, 0, 0), new Health(100, 100));

        // Query again - cache should be invalidated
        int count2 = world.componentQuery()
            .with(Position.class, Velocity.class)
            .count();
        assertEquals(51, count2); // Includes the new entity
    }

    @Test
    void differentQueriesHaveSeparateCacheEntries() {
        for (int i = 0; i < 100; i++) {
            if (i % 2 == 0) {
                world.spawn(new Position(i, 0, 0), new Velocity(1, 0, 0));
            } else {
                world.spawn(new Position(i, 0, 0));
            }
        }

        // Query with two components
        int countPV = world.componentQuery()
            .with(Position.class, Velocity.class)
            .count();

        // Query with one component
        int countP = world.componentQuery()
            .with(Position.class)
            .count();

        assertEquals(50, countPV);
        assertEquals(100, countP);
    }

    // ==================== Bitset Matching Tests ====================

    @Test
    void componentTypeRegistryAssignsUniqueBitIndices() {
        ComponentTypeRegistry registry = new ComponentTypeRegistry();
        
        int posIndex = registry.getBitIndex(Position.class);
        int velIndex = registry.getBitIndex(Velocity.class);
        int healthIndex = registry.getBitIndex(Health.class);

        // All indices should be unique
        assertNotEquals(posIndex, velIndex);
        assertNotEquals(velIndex, healthIndex);
        assertNotEquals(posIndex, healthIndex);
    }

    @Test
    void componentTypeRegistryReturnsSameIndexForSameType() {
        ComponentTypeRegistry registry = new ComponentTypeRegistry();
        
        int index1 = registry.getBitIndex(Position.class);
        int index2 = registry.getBitIndex(Position.class);

        assertEquals(index1, index2);
    }

    @Test
    void bitsetContainsAllCheck() {
        ComponentTypeRegistry registry = new ComponentTypeRegistry();
        
        // Create archetype with Position and Velocity
        BitSet archetypeBits = registry.toBitSet(Set.of(Position.class, Velocity.class));
        
        // Required: just Position
        BitSet requiredBits = registry.toBitSet(Set.of(Position.class));
        
        // Archetype contains Position, so should pass
        assertTrue(ComponentTypeRegistry.containsAll(archetypeBits, requiredBits));
        
        // Required: Position and Health (not in archetype)
        BitSet requiredWithHealth = registry.toBitSet(Set.of(Position.class, Health.class));
        
        // Archetype doesn't have Health, so should fail
        assertFalse(ComponentTypeRegistry.containsAll(archetypeBits, requiredWithHealth));
    }

    @Test
    void bitsetIntersectsCheck() {
        ComponentTypeRegistry registry = new ComponentTypeRegistry();
        
        // Create archetype with Position and Velocity
        BitSet archetypeBits = registry.toBitSet(Set.of(Position.class, Velocity.class));
        
        // Excluded: Health (not in archetype)
        BitSet excludedHealth = registry.toBitSet(Set.of(Health.class));
        assertFalse(ComponentTypeRegistry.intersects(archetypeBits, excludedHealth));
        
        // Excluded: Position (in archetype)
        BitSet excludedPosition = registry.toBitSet(Set.of(Position.class));
        assertTrue(ComponentTypeRegistry.intersects(archetypeBits, excludedPosition));
    }

    @Test
    void bitsetMatchingProducesSameResultsAsSetMatching() {
        // Create mixed entities
        for (int i = 0; i < 100; i++) {
            if (i % 3 == 0) {
                world.spawn(new Position(i, 0, 0), new Velocity(1, 0, 0));
            } else if (i % 3 == 1) {
                world.spawn(new Position(i, 0, 0), new Health(100, 100));
            } else {
                world.spawn(new Position(i, 0, 0), new Velocity(1, 0, 0), new Health(50, 100));
            }
        }

        // Query with inclusion
        int withBoth = world.componentQuery()
            .with(Position.class, Velocity.class)
            .count();
        
        // Query with exclusion
        int withoutHealth = world.componentQuery()
            .with(Position.class, Velocity.class)
            .without(Health.class)
            .count();

        // Expected: 34 entities with Position+Velocity (indices 0,3,6...99 = 34)
        // And 33 entities with Position+Velocity+Health (indices 2,5,8...98 = 33)
        assertEquals(67, withBoth); // 34 + 33
        assertEquals(34, withoutHealth); // Only those without Health
    }

    // ==================== Reader/Writer Caching Tests ====================

    @Test
    void readerWriterCachingWorksCorrectly() {
        world.spawn(new Position(1, 2, 3), new Velocity(4, 5, 6));
        
        // Execute multiple queries - readers/writers should be cached
        for (int i = 0; i < 10; i++) {
            world.componentQuery()
                .withMutable(Position.class)
                .forEach(wrappers -> {
                    @SuppressWarnings("unchecked")
                    Mutable<Position> pos = (Mutable<Position>) wrappers[0];
                    pos.update(p -> new Position(p.x() + 1, p.y(), p.z()));
                }, Position.class);
        }

        // Verify position was updated 10 times
        world.componentQuery()
            .forEach(Position.class, pos -> {
                assertEquals(11.0f, pos.x(), 0.01f); // 1 + 10
            });
    }

    // ==================== Growth Strategy Tests ====================

    @Test
    void archetypeGrowthHandlesManyEntities() {
        // Create 1000 entities - should trigger multiple growth cycles
        for (int i = 0; i < 1000; i++) {
            world.spawn(new Position(i, 0, 0));
        }

        // Verify all entities are accessible
        int count = world.componentQuery()
            .with(Position.class)
            .count();
        assertEquals(1000, count);

        // Verify data integrity
        AtomicInteger sum = new AtomicInteger(0);
        world.componentQuery()
            .forEach(Position.class, pos -> {
                sum.addAndGet((int) pos.x());
            });
        // Sum of 0..999 = (999 * 1000) / 2 = 499500
        assertEquals(499500, sum.get());
    }

    // ==================== QueryCacheKey Tests ====================

    @Test
    void queryCacheKeyEqualityWithSameTypes() {
        Set<Class<?>> included = Set.of(Position.class, Velocity.class);
        Set<Class<?>> excluded = Set.of(Health.class);
        Set<Class<?>> additional = Set.of();

        QueryCacheKey key1 = new QueryCacheKey(included, excluded, additional);
        QueryCacheKey key2 = new QueryCacheKey(included, excluded, additional);

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    void queryCacheKeyDifferentForDifferentTypes() {
        Set<Class<?>> included1 = Set.of(Position.class);
        Set<Class<?>> included2 = Set.of(Position.class, Velocity.class);
        Set<Class<?>> excluded = Set.of();
        Set<Class<?>> additional = Set.of();

        QueryCacheKey key1 = new QueryCacheKey(included1, excluded, additional);
        QueryCacheKey key2 = new QueryCacheKey(included2, excluded, additional);

        assertNotEquals(key1, key2);
    }

    @Test
    void queryCacheKeyDifferentForDifferentExclusions() {
        Set<Class<?>> included = Set.of(Position.class);
        Set<Class<?>> excluded1 = Set.of(Health.class);
        Set<Class<?>> excluded2 = Set.of(Velocity.class);
        Set<Class<?>> additional = Set.of();

        QueryCacheKey key1 = new QueryCacheKey(included, excluded1, additional);
        QueryCacheKey key2 = new QueryCacheKey(included, excluded2, additional);

        assertNotEquals(key1, key2);
    }
}
