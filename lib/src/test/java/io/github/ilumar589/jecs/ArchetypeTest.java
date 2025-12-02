package io.github.ilumar589.jecs;

import io.github.ilumar589.jecs.component.*;
import io.github.ilumar589.jecs.entity.Entity;
import io.github.ilumar589.jecs.world.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Archetype comparing behavior with standard Archetype.
 */
class ArchetypeTest {

    // Static inner class for non-record test
    static class NonRecordComponent {
        int value;
    }

    @Test
    void createArchetypeWithComponentTypes() {
        Set<Class<?>> types = Set.of(Position.class, Velocity.class);
        Archetype archetype = new Archetype(types);

        assertEquals(types, archetype.getComponentTypes());
        assertEquals(0, archetype.size());
    }

    @Test
    void addEntityToArchetype() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        Entity entity = new Entity(1);
        Position position = new Position(1, 2, 3);
        Map<Class<?>, Object> components = Map.of(Position.class, position);

        archetype.addEntity(entity, components);

        assertEquals(1, archetype.size());
        assertTrue(archetype.hasEntity(entity));
        assertEquals(position, archetype.getComponent(entity, Position.class));
    }

    @Test
    void addMultipleEntitiesToArchetype() {
        Set<Class<?>> types = Set.of(Position.class, Velocity.class);
        Archetype archetype = new Archetype(types);

        Entity e1 = new Entity(1);
        Entity e2 = new Entity(2);

        archetype.addEntity(e1, Map.of(
                Position.class, new Position(1, 0, 0),
                Velocity.class, new Velocity(0.1f, 0, 0)
        ));
        archetype.addEntity(e2, Map.of(
                Position.class, new Position(2, 0, 0),
                Velocity.class, new Velocity(0.2f, 0, 0)
        ));

        assertEquals(2, archetype.size());
        assertTrue(archetype.hasEntity(e1));
        assertTrue(archetype.hasEntity(e2));
    }

    @Test
    void removeEntityFromArchetype() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        Entity entity = new Entity(1);
        Position position = new Position(1, 2, 3);
        archetype.addEntity(entity, Map.of(Position.class, position));

        Map<Class<?>, Object> removed = archetype.removeEntity(entity);

        assertEquals(0, archetype.size());
        assertFalse(archetype.hasEntity(entity));
        assertEquals(position, removed.get(Position.class));
    }

    @Test
    void removeEntityFromMiddleOfArchetype() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        Entity e1 = new Entity(1);
        Entity e2 = new Entity(2);
        Entity e3 = new Entity(3);

        archetype.addEntity(e1, Map.of(Position.class, new Position(1, 0, 0)));
        archetype.addEntity(e2, Map.of(Position.class, new Position(2, 0, 0)));
        archetype.addEntity(e3, Map.of(Position.class, new Position(3, 0, 0)));

        archetype.removeEntity(e1);

        assertEquals(2, archetype.size());
        assertFalse(archetype.hasEntity(e1));
        assertTrue(archetype.hasEntity(e2));
        assertTrue(archetype.hasEntity(e3));

        // e2 and e3 should still have correct components
        assertEquals(new Position(2, 0, 0), archetype.getComponent(e2, Position.class));
        assertEquals(new Position(3, 0, 0), archetype.getComponent(e3, Position.class));
    }

    @Test
    void getComponentReturnsNullForNonexistentEntity() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        Entity entity = new Entity(999);
        assertNull(archetype.getComponent(entity, Position.class));
    }

    @Test
    void getComponentReturnsNullForNonexistentType() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        Entity entity = new Entity(1);
        archetype.addEntity(entity, Map.of(Position.class, new Position(1, 2, 3)));

        assertNull(archetype.getComponent(entity, Velocity.class));
    }

    @Test
    void setComponentUpdatesValue() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        Entity entity = new Entity(1);
        archetype.addEntity(entity, Map.of(Position.class, new Position(1, 2, 3)));

        Position newPosition = new Position(10, 20, 30);
        archetype.setComponent(entity, newPosition);

        assertEquals(newPosition, archetype.getComponent(entity, Position.class));
    }

    @Test
    void getEntitiesReturnsAllEntities() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        Entity e1 = new Entity(1);
        Entity e2 = new Entity(2);

        archetype.addEntity(e1, Map.of(Position.class, new Position(1, 0, 0)));
        archetype.addEntity(e2, Map.of(Position.class, new Position(2, 0, 0)));

        List<Entity> entities = archetype.getEntities();
        assertEquals(2, entities.size());
        assertTrue(entities.contains(e1));
        assertTrue(entities.contains(e2));
    }

    @Test
    void addDuplicateEntityThrowsException() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        Entity entity = new Entity(1);
        archetype.addEntity(entity, Map.of(Position.class, new Position(1, 2, 3)));

        assertThrows(IllegalArgumentException.class, () -> 
                archetype.addEntity(entity, Map.of(Position.class, new Position(4, 5, 6))));
    }

    @Test
    void addEntityWithWrongComponentTypesThrowsException() {
        Set<Class<?>> types = Set.of(Position.class, Velocity.class);
        Archetype archetype = new Archetype(types);

        Entity entity = new Entity(1);

        assertThrows(IllegalArgumentException.class, () -> 
                archetype.addEntity(entity, Map.of(Position.class, new Position(1, 2, 3))));
    }

    @Test
    void removeNonexistentEntityThrowsException() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        assertThrows(IllegalArgumentException.class, () -> archetype.removeEntity(new Entity(999)));
    }

    @Test
    void archetypesWithSameTypesAreEqual() {
        Set<Class<?>> types = Set.of(Position.class, Velocity.class);
        Archetype a1 = new Archetype(types);
        Archetype a2 = new Archetype(types);

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    void archetypesWithDifferentTypesAreNotEqual() {
        Archetype a1 = new Archetype(Set.of(Position.class));
        Archetype a2 = new Archetype(Set.of(Velocity.class));

        assertNotEquals(a1, a2);
    }

    // --- ComponentReader tests ---

    @Test
    void componentReaderReadsCorrectValues() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        Position p1 = new Position(1, 2, 3);
        Position p2 = new Position(4, 5, 6);

        archetype.addEntity(new Entity(1), Map.of(Position.class, p1));
        archetype.addEntity(new Entity(2), Map.of(Position.class, p2));

        ComponentReader<Position> reader = archetype.getReader(Position.class);

        assertEquals(2, reader.size());
        assertEquals(p1, reader.read(0));
        assertEquals(p2, reader.read(1));
    }

    @Test
    void componentReaderReadBatch() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        for (int i = 0; i < 10; i++) {
            archetype.addEntity(new Entity(i), Map.of(Position.class, new Position(i, i * 2, i * 3)));
        }

        ComponentReader<Position> reader = archetype.getReader(Position.class);
        List<Position> collected = new ArrayList<>();
        reader.readBatch(3, 4, collected::add);

        assertEquals(4, collected.size());
        assertEquals(new Position(3, 6, 9), collected.get(0));
        assertEquals(new Position(4, 8, 12), collected.get(1));
        assertEquals(new Position(5, 10, 15), collected.get(2));
        assertEquals(new Position(6, 12, 18), collected.get(3));
    }

    @Test
    void componentReaderThrowsOnInvalidIndex() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        archetype.addEntity(new Entity(1), Map.of(Position.class, new Position(1, 2, 3)));

        ComponentReader<Position> reader = archetype.getReader(Position.class);

        assertThrows(IndexOutOfBoundsException.class, () -> reader.read(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> reader.read(1));
        assertThrows(IndexOutOfBoundsException.class, () -> reader.read(100));
    }

    @Test
    void componentReaderForNonexistentTypeThrows() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        assertThrows(IllegalArgumentException.class, () -> archetype.getReader(Velocity.class));
    }

    // --- ComponentWriter tests ---

    @Test
    void componentWriterWritesCorrectValues() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        archetype.addEntity(new Entity(1), Map.of(Position.class, new Position(1, 2, 3)));

        ComponentWriter<Position> writer = archetype.getWriter(Position.class);
        Position newPos = new Position(10, 20, 30);
        writer.write(0, newPos);

        assertEquals(newPos, archetype.getComponent(new Entity(1), Position.class));
    }

    @Test
    void componentWriterSize() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        archetype.addEntity(new Entity(1), Map.of(Position.class, new Position(1, 2, 3)));
        archetype.addEntity(new Entity(2), Map.of(Position.class, new Position(4, 5, 6)));

        ComponentWriter<Position> writer = archetype.getWriter(Position.class);
        assertEquals(2, writer.size());
    }

    @Test
    void componentWriterThrowsOnInvalidIndex() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        archetype.addEntity(new Entity(1), Map.of(Position.class, new Position(1, 2, 3)));

        ComponentWriter<Position> writer = archetype.getWriter(Position.class);

        assertThrows(IndexOutOfBoundsException.class, () -> writer.write(-1, new Position(0, 0, 0)));
        assertThrows(IndexOutOfBoundsException.class, () -> writer.write(1, new Position(0, 0, 0)));
    }

    @Test
    void componentWriterForNonexistentTypeThrows() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        assertThrows(IllegalArgumentException.class, () -> archetype.getWriter(Velocity.class));
    }

    // --- Multiple component tests ---

    @Test
    void multipleComponentTypesStoredCorrectly() {
        Set<Class<?>> types = Set.of(Position.class, Velocity.class, Health.class);
        Archetype archetype = new Archetype(types);

        Entity e = new Entity(1);
        Position pos = new Position(1, 2, 3);
        Velocity vel = new Velocity(0.1f, 0.2f, 0.3f);
        Health health = new Health(80, 100);

        archetype.addEntity(e, Map.of(
                Position.class, pos,
                Velocity.class, vel,
                Health.class, health
        ));

        assertEquals(pos, archetype.getComponent(e, Position.class));
        assertEquals(vel, archetype.getComponent(e, Velocity.class));
        assertEquals(health, archetype.getComponent(e, Health.class));
    }

    @Test
    void mixedPrimitiveTypesHandledCorrectly() {
        // Position has floats, Health has ints
        Set<Class<?>> types = Set.of(Position.class, Health.class);
        Archetype archetype = new Archetype(types);

        for (int i = 0; i < 100; i++) {
            archetype.addEntity(new Entity(i), Map.of(
                    Position.class, new Position(i, i * 2, i * 3),
                    Health.class, new Health(100 - i, 100)
            ));
        }

        assertEquals(100, archetype.size());

        // Verify a few values
        assertEquals(new Position(50, 100, 150), archetype.getComponent(new Entity(50), Position.class));
        assertEquals(new Health(50, 100), archetype.getComponent(new Entity(50), Health.class));
    }

    // --- Capacity/resize tests ---

    @Test
    void archetypeGrowsWhenCapacityExceeded() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types, 4);

        // Add more entities than initial capacity
        for (int i = 0; i < 100; i++) {
            archetype.addEntity(new Entity(i), Map.of(Position.class, new Position(i, 0, 0)));
        }

        assertEquals(100, archetype.size());
        assertTrue(archetype.capacity() >= 100);

        // Verify all values are still correct after resize
        for (int i = 0; i < 100; i++) {
            assertEquals(new Position(i, 0, 0), archetype.getComponent(new Entity(i), Position.class));
        }
    }

    // --- Movement simulation test (usage pattern from requirements) ---

    @Test
    void movementSystemUsagePattern() {
        Set<Class<?>> types = Set.of(Position.class, Velocity.class);
        Archetype archetype = new Archetype(types, 100);

        // Create 100 entities with positions and velocities
        for (int i = 0; i < 100; i++) {
            archetype.addEntity(new Entity(i), Map.of(
                    Position.class, new Position(0, 0, 0),
                    Velocity.class, new Velocity(1, 2, 3)
            ));
        }

        float dt = 0.016f;  // ~60fps

        // Simulate one frame of movement
        ComponentReader<Position> posReader = archetype.getReader(Position.class);
        ComponentReader<Velocity> velReader = archetype.getReader(Velocity.class);
        ComponentWriter<Position> posWriter = archetype.getWriter(Position.class);

        for (int i = 0; i < archetype.size(); i++) {
            Position pos = posReader.read(i);
            Velocity vel = velReader.read(i);

            Position newPos = new Position(
                    pos.x() + vel.dx() * dt,
                    pos.y() + vel.dy() * dt,
                    pos.z() + vel.dz() * dt
            );

            posWriter.write(i, newPos);
        }

        // Verify positions were updated
        Position expected = new Position(
                0 + 1 * dt,
                0 + 2 * dt,
                0 + 3 * dt
        );

        for (int i = 0; i < 100; i++) {
            Position actual = archetype.getComponent(new Entity(i), Position.class);
            assertEquals(expected.x(), actual.x(), 0.0001f);
            assertEquals(expected.y(), actual.y(), 0.0001f);
            assertEquals(expected.z(), actual.z(), 0.0001f);
        }
    }

    // --- Record validation tests ---

    @Test
    void nonRecordTypeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                new Archetype(Set.of(NonRecordComponent.class)));
    }

    // --- GlobalArray tests (replacing PrimitiveType tests) ---

    @Test
    void globalArrayIsSupported() {
        assertTrue(GlobalArray.isSupported(int.class));
        assertTrue(GlobalArray.isSupported(Integer.class));
        assertTrue(GlobalArray.isSupported(float.class));
        assertTrue(GlobalArray.isSupported(Float.class));
        assertTrue(GlobalArray.isSupported(double.class));
        assertTrue(GlobalArray.isSupported(long.class));
        assertTrue(GlobalArray.isSupported(boolean.class));
        assertTrue(GlobalArray.isSupported(byte.class));
        assertTrue(GlobalArray.isSupported(short.class));
        assertTrue(GlobalArray.isSupported(char.class));
        assertTrue(GlobalArray.isSupported(String.class));
        assertFalse(GlobalArray.isSupported(Object.class));
    }

    @Test
    void globalArrayFromClass() {
        assertInstanceOf(GlobalArray.IntArray.class, GlobalArray.fromClass(int.class, 10));
        assertInstanceOf(GlobalArray.IntArray.class, GlobalArray.fromClass(Integer.class, 10));
        assertInstanceOf(GlobalArray.FloatArray.class, GlobalArray.fromClass(float.class, 10));
        assertInstanceOf(GlobalArray.FloatArray.class, GlobalArray.fromClass(Float.class, 10));
        assertInstanceOf(GlobalArray.DoubleArray.class, GlobalArray.fromClass(double.class, 10));
        assertInstanceOf(GlobalArray.LongArray.class, GlobalArray.fromClass(long.class, 10));
        assertInstanceOf(GlobalArray.BooleanArray.class, GlobalArray.fromClass(boolean.class, 10));
        assertInstanceOf(GlobalArray.StringArray.class, GlobalArray.fromClass(String.class, 10));
        assertNull(GlobalArray.fromClass(Object.class, 10));
    }

    @Test
    void globalArrayReadWriteOperations() {
        GlobalArray.IntArray intArray = (GlobalArray.IntArray) GlobalArray.fromClass(int.class, 10);
        assertNotNull(intArray);
        intArray.writeRelease(5, 42);
        assertEquals(42, intArray.readPlain(5));
        
        GlobalArray.FloatArray floatArray = (GlobalArray.FloatArray) GlobalArray.fromClass(float.class, 10);
        assertNotNull(floatArray);
        floatArray.writeRelease(3, 3.14f);
        assertEquals(3.14f, floatArray.readPlain(3), 0.001f);
        
        GlobalArray.StringArray stringArray = (GlobalArray.StringArray) GlobalArray.fromClass(String.class, 10);
        assertNotNull(stringArray);
        stringArray.writeRelease(0, "hello");
        assertEquals("hello", stringArray.readPlain(0));
    }

    // --- FieldKey tests ---

    @Test
    void fieldKeyEquality() {
        FieldKey key1 = new FieldKey(Position.class, "x", float.class);
        FieldKey key2 = new FieldKey(Position.class, "x", float.class);
        FieldKey key3 = new FieldKey(Velocity.class, "x", float.class);

        assertEquals(key1, key2);
        assertNotEquals(key1, key3);  // Different component type
    }

    @Test
    void fieldKeyValidation() {
        assertThrows(IllegalArgumentException.class, () -> 
                new FieldKey(null, "x", float.class));
        assertThrows(IllegalArgumentException.class, () -> 
                new FieldKey(Position.class, null, float.class));
        assertThrows(IllegalArgumentException.class, () -> 
                new FieldKey(Position.class, "", float.class));
        assertThrows(IllegalArgumentException.class, () -> 
                new FieldKey(Position.class, "x", null));
        assertThrows(IllegalArgumentException.class, () -> 
                new FieldKey(Position.class, "x", Object.class));  // Unsupported type
    }

    // --- ReadAll test ---

    @Test
    void componentReaderReadAll() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        for (int i = 0; i < 5; i++) {
            archetype.addEntity(new Entity(i), Map.of(Position.class, new Position(i, 0, 0)));
        }

        ComponentReader<Position> reader = archetype.getReader(Position.class);
        List<Position> collected = new ArrayList<>();
        reader.readAll(collected::add);

        assertEquals(5, collected.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(new Position(i, 0, 0), collected.get(i));
        }
    }

    // --- Extended array resizing tests ---

    @Test
    void multipleResizesPreserveData() {
        Set<Class<?>> types = Set.of(Position.class, Health.class);
        Archetype archetype = new Archetype(types, 2);  // Start very small

        // Add entities in batches, triggering multiple resizes
        for (int i = 0; i < 1000; i++) {
            archetype.addEntity(new Entity(i), Map.of(
                    Position.class, new Position(i, i * 2, i * 3),
                    Health.class, new Health(i, 100)
            ));
        }

        assertEquals(1000, archetype.size());
        assertTrue(archetype.capacity() >= 1000);

        // Verify all data survived resizes correctly
        for (int i = 0; i < 1000; i++) {
            Entity entity = new Entity(i);
            Position pos = archetype.getComponent(entity, Position.class);
            Health health = archetype.getComponent(entity, Health.class);

            assertEquals(new Position(i, i * 2, i * 3), pos);
            assertEquals(new Health(i, 100), health);
        }
    }

    @Test
    void resizeWithMultiplePrimitiveTypes() {
        // Test with multiple components having different primitive types (floats and ints)
        Set<Class<?>> types = Set.of(Position.class, Health.class);
        Archetype archetype = new Archetype(types, 2);

        for (int i = 0; i < 100; i++) {
            archetype.addEntity(new Entity(i), Map.of(
                    Position.class, new Position(i, i * 0.5f, i * 1.5f),
                    Health.class, new Health(i, i * 2)
            ));
        }

        assertEquals(100, archetype.size());

        // Verify all field types survived resize
        for (int i = 0; i < 100; i++) {
            Position pos = archetype.getComponent(new Entity(i), Position.class);
            Health health = archetype.getComponent(new Entity(i), Health.class);
            assertEquals(i, pos.x(), 0.0001f);
            assertEquals(i * 0.5f, pos.y(), 0.0001f);
            assertEquals(i * 1.5f, pos.z(), 0.0001f);
            assertEquals(i, health.current());
            assertEquals(i * 2, health.max());
        }
    }

    @Test
    void resizeThenRemovePreservesCorrectData() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types, 4);

        // Add entities to trigger resize
        for (int i = 0; i < 50; i++) {
            archetype.addEntity(new Entity(i), Map.of(Position.class, new Position(i, 0, 0)));
        }

        // Remove some entities (swap-and-pop)
        for (int i = 0; i < 25; i++) {
            archetype.removeEntity(new Entity(i));
        }

        assertEquals(25, archetype.size());

        // Verify remaining entities have correct data
        for (int i = 25; i < 50; i++) {
            Position pos = archetype.getComponent(new Entity(i), Position.class);
            assertEquals(new Position(i, 0, 0), pos);
        }
    }

    @Test
    void readerWriterStayValidAfterResize() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types, 4);

        // Get reader/writer before resize
        ComponentReader<Position> reader = archetype.getReader(Position.class);
        ComponentWriter<Position> writer = archetype.getWriter(Position.class);

        // Add entities to trigger resize
        for (int i = 0; i < 50; i++) {
            archetype.addEntity(new Entity(i), Map.of(Position.class, new Position(i, 0, 0)));
        }

        // Reader/writer should still work after resize
        assertEquals(50, reader.size());
        assertEquals(50, writer.size());

        // Verify reader works
        assertEquals(new Position(25, 0, 0), reader.read(25));

        // Verify writer works
        writer.write(25, new Position(100, 200, 300));
        assertEquals(new Position(100, 200, 300), reader.read(25));
    }

    // --- Concurrent read tests ---

    @Test
    void concurrentReadersDoNotInterfere() throws Exception {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types, 1000);

        // Add test data
        for (int i = 0; i < 1000; i++) {
            archetype.addEntity(new Entity(i), Map.of(
                    Position.class, new Position(i, i * 2, i * 3)
            ));
        }

        int threadCount = 4;
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicBoolean failed = new java.util.concurrent.atomic.AtomicBoolean(false);

        // Each thread gets its own reader and iterates through all entities
        for (int t = 0; t < threadCount; t++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    ComponentReader<Position> reader = archetype.getReader(Position.class);

                    for (int i = 0; i < 1000; i++) {
                        Position pos = reader.read(i);
                        if (pos.x() != i || pos.y() != i * 2 || pos.z() != i * 3) {
                            failed.set(true);
                            break;
                        }
                    }
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();  // Start all threads
        doneLatch.await();       // Wait for all to finish

        assertFalse(failed.get(), "Concurrent readers should read correct values");
    }

    @Test
    void readBatchWorksWithMultipleThreads() throws Exception {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types, 1000);

        for (int i = 0; i < 1000; i++) {
            archetype.addEntity(new Entity(i), Map.of(
                    Position.class, new Position(i, 0, 0)
            ));
        }

        int threadCount = 4;
        int batchSize = 250;  // Each thread processes a quarter
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicBoolean failed = new java.util.concurrent.atomic.AtomicBoolean(false);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startLatch.await();
                    ComponentReader<Position> reader = archetype.getReader(Position.class);

                    int start = threadId * batchSize;
                    List<Position> collected = new ArrayList<>();
                    reader.readBatch(start, batchSize, collected::add);

                    // Verify batch results
                    for (int i = 0; i < batchSize; i++) {
                        if (collected.get(i).x() != start + i) {
                            failed.set(true);
                            break;
                        }
                    }
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        assertFalse(failed.get(), "readBatch should work correctly with multiple threads");
    }

    @Test
    void multipleReadersMultipleComponentTypes() throws Exception {
        Set<Class<?>> types = Set.of(Position.class, Velocity.class, Health.class);
        Archetype archetype = new Archetype(types, 500);

        for (int i = 0; i < 500; i++) {
            archetype.addEntity(new Entity(i), Map.of(
                    Position.class, new Position(i, i, i),
                    Velocity.class, new Velocity(i * 0.1f, i * 0.2f, i * 0.3f),
                    Health.class, new Health(i, 100)
            ));
        }

        int threadCount = 6;  // 2 threads per component type
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicBoolean failed = new java.util.concurrent.atomic.AtomicBoolean(false);

        // Position readers
        for (int t = 0; t < 2; t++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    ComponentReader<Position> reader = archetype.getReader(Position.class);
                    for (int i = 0; i < 500; i++) {
                        Position pos = reader.read(i);
                        if (pos.x() != i) {
                            failed.set(true);
                        }
                    }
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Velocity readers
        for (int t = 0; t < 2; t++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    ComponentReader<Velocity> reader = archetype.getReader(Velocity.class);
                    for (int i = 0; i < 500; i++) {
                        Velocity vel = reader.read(i);
                        if (Math.abs(vel.dx() - i * 0.1f) > 0.001f) {
                            failed.set(true);
                        }
                    }
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Health readers
        for (int t = 0; t < 2; t++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    ComponentReader<Health> reader = archetype.getReader(Health.class);
                    for (int i = 0; i < 500; i++) {
                        Health health = reader.read(i);
                        if (health.current() != i) {
                            failed.set(true);
                        }
                    }
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        assertFalse(failed.get(), "Multiple readers on different component types should work correctly");
    }

    // --- Edge case tests ---

    @Test
    void removeAllEntitiesThenAddNew() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        // Add and remove all
        for (int i = 0; i < 10; i++) {
            archetype.addEntity(new Entity(i), Map.of(Position.class, new Position(i, 0, 0)));
        }
        for (int i = 0; i < 10; i++) {
            archetype.removeEntity(new Entity(i));
        }

        assertEquals(0, archetype.size());

        // Add new entities
        for (int i = 100; i < 110; i++) {
            archetype.addEntity(new Entity(i), Map.of(Position.class, new Position(i, 0, 0)));
        }

        assertEquals(10, archetype.size());
        assertEquals(new Position(105, 0, 0), archetype.getComponent(new Entity(105), Position.class));
    }

    @Test
    void addRemoveAddPatternPreservesIntegrity() {
        Set<Class<?>> types = Set.of(Position.class, Health.class);
        Archetype archetype = new Archetype(types, 4);

        // Add 10 entities
        for (int i = 0; i < 10; i++) {
            archetype.addEntity(new Entity(i), Map.of(
                    Position.class, new Position(i, 0, 0),
                    Health.class, new Health(i, 100)
            ));
        }

        // Remove odd entities
        for (int i = 1; i < 10; i += 2) {
            archetype.removeEntity(new Entity(i));
        }

        // Add more entities
        for (int i = 100; i < 110; i++) {
            archetype.addEntity(new Entity(i), Map.of(
                    Position.class, new Position(i, 0, 0),
                    Health.class, new Health(i, 100)
            ));
        }

        assertEquals(15, archetype.size());

        // Verify even entities from first batch
        for (int i = 0; i < 10; i += 2) {
            assertEquals(new Position(i, 0, 0), archetype.getComponent(new Entity(i), Position.class));
            assertEquals(new Health(i, 100), archetype.getComponent(new Entity(i), Health.class));
        }

        // Verify new entities
        for (int i = 100; i < 110; i++) {
            assertEquals(new Position(i, 0, 0), archetype.getComponent(new Entity(i), Position.class));
            assertEquals(new Health(i, 100), archetype.getComponent(new Entity(i), Health.class));
        }
    }

    @Test
    void swapPopRemovalPreservesFieldAlignment() {
        // Test that swap-and-pop maintains correct field alignment across component types
        // Using Position (floats) and Health (ints) together
        Set<Class<?>> types = Set.of(Position.class, Health.class);
        Archetype archetype = new Archetype(types);

        for (int i = 0; i < 10; i++) {
            archetype.addEntity(new Entity(i), Map.of(
                    Position.class, new Position(i, i * 0.1f, i * 0.2f),
                    Health.class, new Health(i * 10, 100)
            ));
        }

        // Remove first entity (triggers swap with last)
        archetype.removeEntity(new Entity(0));

        // Entity 9 should now be at index 0 (swapped)
        Position swappedPos = archetype.getComponent(new Entity(9), Position.class);
        Health swappedHealth = archetype.getComponent(new Entity(9), Health.class);
        assertNotNull(swappedPos);
        assertNotNull(swappedHealth);
        assertEquals(9, swappedPos.x(), 0.0001f);
        assertEquals(9 * 0.1f, swappedPos.y(), 0.0001f);
        assertEquals(9 * 0.2f, swappedPos.z(), 0.0001f);
        assertEquals(90, swappedHealth.current());
        assertEquals(100, swappedHealth.max());

        // All other entities should be unchanged
        for (int i = 1; i < 9; i++) {
            Position pos = archetype.getComponent(new Entity(i), Position.class);
            Health health = archetype.getComponent(new Entity(i), Health.class);
            assertNotNull(pos);
            assertNotNull(health);
            assertEquals(i, pos.x(), 0.0001f);
            assertEquals(i * 10, health.current());
        }
    }

    // ============================================================================
    // COMPREHENSIVE SYSTEM EXAMPLES
    // These tests demonstrate realistic ECS usage patterns with multiple systems
    // ============================================================================

    /**
     * Example: Movement System
     * Queries all entities with Position and Velocity, updates positions based on velocity.
     */
    @Test
    void exampleMovementSystem() {
        // Setup: Create archetype with Position and Velocity components
        Set<Class<?>> types = Set.of(Position.class, Velocity.class);
        Archetype archetype = new Archetype(types, 100);

        // Create entities with different positions and velocities
        archetype.addEntity(new Entity(1), Map.of(
                Position.class, new Position(0, 0, 0),
                Velocity.class, new Velocity(1, 0, 0)  // Moving right
        ));
        archetype.addEntity(new Entity(2), Map.of(
                Position.class, new Position(10, 10, 0),
                Velocity.class, new Velocity(0, -2, 0)  // Moving down
        ));
        archetype.addEntity(new Entity(3), Map.of(
                Position.class, new Position(5, 5, 5),
                Velocity.class, new Velocity(1, 1, 1)  // Moving diagonally
        ));

        float deltaTime = 1.0f;  // 1 second timestep

        // ========== MOVEMENT SYSTEM ==========
        // This is how a typical ECS system would process entities
        System.out.println("=== Movement System Processing ===");

        ComponentReader<Position> posReader = archetype.getReader(Position.class);
        ComponentReader<Velocity> velReader = archetype.getReader(Velocity.class);
        ComponentWriter<Position> posWriter = archetype.getWriter(Position.class);
        List<Entity> entities = archetype.getEntities();

        for (int i = 0; i < archetype.size(); i++) {
            Position pos = posReader.read(i);
            Velocity vel = velReader.read(i);
            Entity entity = entities.get(i);

            // Print current state
            System.out.printf("Entity %d: Position(%.1f, %.1f, %.1f) + Velocity(%.1f, %.1f, %.1f) * dt=%.1f%n",
                    entity.id(), pos.x(), pos.y(), pos.z(), vel.dx(), vel.dy(), vel.dz(), deltaTime);

            // Calculate new position
            Position newPos = new Position(
                    pos.x() + vel.dx() * deltaTime,
                    pos.y() + vel.dy() * deltaTime,
                    pos.z() + vel.dz() * deltaTime
            );

            // Write updated position
            posWriter.write(i, newPos);

            System.out.printf("  -> New Position(%.1f, %.1f, %.1f)%n", newPos.x(), newPos.y(), newPos.z());
        }

        // Verify results
        assertEquals(new Position(1, 0, 0), archetype.getComponent(new Entity(1), Position.class));
        assertEquals(new Position(10, 8, 0), archetype.getComponent(new Entity(2), Position.class));
        assertEquals(new Position(6, 6, 6), archetype.getComponent(new Entity(3), Position.class));
    }

    /**
     * Example: Health and Damage System
     * Demonstrates querying entities with Health component and applying damage.
     */
    @Test
    void exampleHealthAndDamageSystem() {
        // Setup: Create archetype with Position and Health components
        Set<Class<?>> types = Set.of(Position.class, Health.class);
        Archetype archetype = new Archetype(types);

        // Create entities: players and enemies at different positions with different health
        archetype.addEntity(new Entity(1), Map.of(
                Position.class, new Position(0, 0, 0),
                Health.class, new Health(100, 100)  // Full health player
        ));
        archetype.addEntity(new Entity(2), Map.of(
                Position.class, new Position(5, 0, 0),
                Health.class, new Health(50, 100)  // Damaged player
        ));
        archetype.addEntity(new Entity(3), Map.of(
                Position.class, new Position(10, 0, 0),
                Health.class, new Health(30, 50)   // Weak enemy
        ));
        archetype.addEntity(new Entity(4), Map.of(
                Position.class, new Position(15, 0, 0),
                Health.class, new Health(200, 200) // Boss
        ));

        // ========== DAMAGE SYSTEM ==========
        // Apply 25 damage to all entities
        System.out.println("\n=== Damage System Processing ===");
        int damage = 25;

        ComponentReader<Position> posReader = archetype.getReader(Position.class);
        ComponentReader<Health> healthReader = archetype.getReader(Health.class);
        ComponentWriter<Health> healthWriter = archetype.getWriter(Health.class);
        List<Entity> entities = archetype.getEntities();

        for (int i = 0; i < archetype.size(); i++) {
            Position pos = posReader.read(i);
            Health health = healthReader.read(i);
            Entity entity = entities.get(i);

            System.out.printf("Entity %d at Position(%.1f, %.1f, %.1f): Health %d/%d%n",
                    entity.id(), pos.x(), pos.y(), pos.z(), health.current(), health.max());

            // Apply damage
            int newHealth = Math.max(0, health.current() - damage);
            healthWriter.write(i, new Health(newHealth, health.max()));

            System.out.printf("  -> Took %d damage, now %d/%d%s%n",
                    damage, newHealth, health.max(), newHealth == 0 ? " [DEAD]" : "");
        }

        // Verify results
        assertEquals(new Health(75, 100), archetype.getComponent(new Entity(1), Health.class));
        assertEquals(new Health(25, 100), archetype.getComponent(new Entity(2), Health.class));
        assertEquals(new Health(5, 50), archetype.getComponent(new Entity(3), Health.class));
        assertEquals(new Health(175, 200), archetype.getComponent(new Entity(4), Health.class));
    }

    /**
     * Example: Multiple Systems Working Together
     * Demonstrates how multiple systems can query and process the same archetype.
     */
    @Test
    void exampleMultipleSystemsWorkingTogether() {
        // Setup: Create archetype with all three component types
        Set<Class<?>> types = Set.of(Position.class, Velocity.class, Health.class);
        Archetype archetype = new Archetype(types);

        // Create game entities
        archetype.addEntity(new Entity(1), Map.of(
                Position.class, new Position(0, 0, 0),
                Velocity.class, new Velocity(2, 0, 0),
                Health.class, new Health(100, 100)
        ));
        archetype.addEntity(new Entity(2), Map.of(
                Position.class, new Position(100, 0, 0),
                Velocity.class, new Velocity(-3, 0, 0),
                Health.class, new Health(80, 100)
        ));
        archetype.addEntity(new Entity(3), Map.of(
                Position.class, new Position(50, 50, 0),
                Velocity.class, new Velocity(0, -1, 0),
                Health.class, new Health(60, 60)
        ));

        float deltaTime = 0.5f;

        System.out.println("\n=== Frame Start ===");

        // ========== SYSTEM 1: Movement ==========
        System.out.println("\n--- System 1: Movement ---");
        {
            ComponentReader<Position> posReader = archetype.getReader(Position.class);
            ComponentReader<Velocity> velReader = archetype.getReader(Velocity.class);
            ComponentWriter<Position> posWriter = archetype.getWriter(Position.class);

            for (int i = 0; i < archetype.size(); i++) {
                Position pos = posReader.read(i);
                Velocity vel = velReader.read(i);

                Position newPos = new Position(
                        pos.x() + vel.dx() * deltaTime,
                        pos.y() + vel.dy() * deltaTime,
                        pos.z() + vel.dz() * deltaTime
                );
                posWriter.write(i, newPos);

                System.out.printf("Entity moved: (%.1f, %.1f, %.1f) -> (%.1f, %.1f, %.1f)%n",
                        pos.x(), pos.y(), pos.z(), newPos.x(), newPos.y(), newPos.z());
            }
        }

        // ========== SYSTEM 2: Health Regeneration ==========
        System.out.println("\n--- System 2: Health Regeneration ---");
        {
            ComponentReader<Health> healthReader = archetype.getReader(Health.class);
            ComponentWriter<Health> healthWriter = archetype.getWriter(Health.class);
            int regenAmount = 5;

            for (int i = 0; i < archetype.size(); i++) {
                Health health = healthReader.read(i);

                if (health.current() < health.max()) {
                    int newCurrent = Math.min(health.max(), health.current() + regenAmount);
                    healthWriter.write(i, new Health(newCurrent, health.max()));
                    System.out.printf("Entity regenerated: %d/%d -> %d/%d%n",
                            health.current(), health.max(), newCurrent, health.max());
                } else {
                    System.out.printf("Entity at full health: %d/%d%n", health.current(), health.max());
                }
            }
        }

        // ========== SYSTEM 3: Debug Print All Components ==========
        System.out.println("\n--- System 3: Debug Print State ---");
        {
            ComponentReader<Position> posReader = archetype.getReader(Position.class);
            ComponentReader<Velocity> velReader = archetype.getReader(Velocity.class);
            ComponentReader<Health> healthReader = archetype.getReader(Health.class);
            List<Entity> entities = archetype.getEntities();

            for (int i = 0; i < archetype.size(); i++) {
                Entity entity = entities.get(i);
                Position pos = posReader.read(i);
                Velocity vel = velReader.read(i);
                Health health = healthReader.read(i);

                System.out.printf("Entity %d: Pos(%.1f, %.1f, %.1f), Vel(%.1f, %.1f, %.1f), HP(%d/%d)%n",
                        entity.id(),
                        pos.x(), pos.y(), pos.z(),
                        vel.dx(), vel.dy(), vel.dz(),
                        health.current(), health.max());
            }
        }

        System.out.println("\n=== Frame End ===");

        // Verify final state
        // Entity 1: moved from (0,0,0) by vel(2,0,0)*0.5 = (1,0,0), health 100->100 (full)
        assertEquals(new Position(1, 0, 0), archetype.getComponent(new Entity(1), Position.class));
        assertEquals(new Health(100, 100), archetype.getComponent(new Entity(1), Health.class));

        // Entity 2: moved from (100,0,0) by vel(-3,0,0)*0.5 = (98.5,0,0), health 80->85 (regen)
        Position pos2 = archetype.getComponent(new Entity(2), Position.class);
        assertEquals(98.5f, pos2.x(), 0.001f);
        assertEquals(new Health(85, 100), archetype.getComponent(new Entity(2), Health.class));

        // Entity 3: moved from (50,50,0) by vel(0,-1,0)*0.5 = (50,49.5,0), health 60->60 (full)
        Position pos3 = archetype.getComponent(new Entity(3), Position.class);
        assertEquals(49.5f, pos3.y(), 0.001f);
        assertEquals(new Health(60, 60), archetype.getComponent(new Entity(3), Health.class));
    }

    /**
     * Example: Filtering and Processing Subsets
     * Demonstrates querying entities and processing only those matching certain criteria.
     */
    @Test
    void exampleFilteringAndProcessingSubsets() {
        Set<Class<?>> types = Set.of(Position.class, Health.class);
        Archetype archetype = new Archetype(types);

        // Create a mix of entities with different health levels
        for (int i = 0; i < 10; i++) {
            archetype.addEntity(new Entity(i), Map.of(
                    Position.class, new Position(i * 10, 0, 0),
                    Health.class, new Health(i * 10 + 10, 100)  // Health: 10, 20, 30, ..., 100
            ));
        }

        System.out.println("\n=== Filtering System: Find Low Health Entities ===");

        // ========== SYSTEM: Find and Heal Low Health Entities ==========
        ComponentReader<Position> posReader = archetype.getReader(Position.class);
        ComponentReader<Health> healthReader = archetype.getReader(Health.class);
        ComponentWriter<Health> healthWriter = archetype.getWriter(Health.class);
        List<Entity> entities = archetype.getEntities();

        int lowHealthThreshold = 50;
        int healAmount = 20;
        int healedCount = 0;

        System.out.printf("Looking for entities with health < %d...%n", lowHealthThreshold);

        for (int i = 0; i < archetype.size(); i++) {
            Health health = healthReader.read(i);

            if (health.current() < lowHealthThreshold) {
                Position pos = posReader.read(i);
                Entity entity = entities.get(i);

                System.out.printf("Found low health entity %d at (%.0f, %.0f, %.0f) with %d/%d HP%n",
                        entity.id(), pos.x(), pos.y(), pos.z(), health.current(), health.max());

                // Heal them
                int newHealth = Math.min(health.max(), health.current() + healAmount);
                healthWriter.write(i, new Health(newHealth, health.max()));
                System.out.printf("  -> Healed to %d/%d HP%n", newHealth, health.max());
                healedCount++;
            }
        }

        System.out.printf("Healed %d entities%n", healedCount);

        // Verify: entities with original health < 50 (IDs 0-3 with health 10,20,30,40) should be healed
        assertEquals(4, healedCount);
        assertEquals(new Health(30, 100), archetype.getComponent(new Entity(0), Health.class));  // 10 + 20
        assertEquals(new Health(40, 100), archetype.getComponent(new Entity(1), Health.class));  // 20 + 20
        assertEquals(new Health(50, 100), archetype.getComponent(new Entity(2), Health.class));  // 30 + 20
        assertEquals(new Health(60, 100), archetype.getComponent(new Entity(3), Health.class));  // 40 + 20
        // Entities 4-9 should be unchanged
        assertEquals(new Health(50, 100), archetype.getComponent(new Entity(4), Health.class));
    }

    /**
     * Example: Batch Processing with readAll
     * Demonstrates using readAll for bulk operations.
     */
    @Test
    void exampleBatchProcessingWithReadAll() {
        Set<Class<?>> types = Set.of(Position.class, Velocity.class);
        Archetype archetype = new Archetype(types);

        // Create entities
        for (int i = 0; i < 5; i++) {
            archetype.addEntity(new Entity(i), Map.of(
                    Position.class, new Position(i, i * 2, i * 3),
                    Velocity.class, new Velocity(1, 1, 1)
            ));
        }

        System.out.println("\n=== Batch Processing Example ===");

        // ========== Collect all positions using readAll ==========
        System.out.println("Collecting all positions:");
        List<Position> allPositions = new ArrayList<>();
        archetype.getReader(Position.class).readAll(allPositions::add);

        for (int i = 0; i < allPositions.size(); i++) {
            Position pos = allPositions.get(i);
            System.out.printf("  Position[%d]: (%.0f, %.0f, %.0f)%n", i, pos.x(), pos.y(), pos.z());
        }

        // ========== Calculate center of mass ==========
        float sumX = 0, sumY = 0, sumZ = 0;
        for (Position pos : allPositions) {
            sumX += pos.x();
            sumY += pos.y();
            sumZ += pos.z();
        }
        float centerX = sumX / allPositions.size();
        float centerY = sumY / allPositions.size();
        float centerZ = sumZ / allPositions.size();

        System.out.printf("Center of mass: (%.1f, %.1f, %.1f)%n", centerX, centerY, centerZ);

        // Verify
        assertEquals(5, allPositions.size());
        assertEquals(2.0f, centerX, 0.001f);  // (0+1+2+3+4)/5 = 2
        assertEquals(4.0f, centerY, 0.001f);  // (0+2+4+6+8)/5 = 4
        assertEquals(6.0f, centerZ, 0.001f);  // (0+3+6+9+12)/5 = 6
    }

    /**
     * Example: Parallel Batch Processing
     * Demonstrates using readBatch for parallel processing of entity subsets.
     */
    @Test
    void exampleParallelBatchProcessing() throws Exception {
        Set<Class<?>> types = Set.of(Position.class, Health.class);
        Archetype archetype = new Archetype(types, 1000);

        // Create 1000 entities
        for (int i = 0; i < 1000; i++) {
            archetype.addEntity(new Entity(i), Map.of(
                    Position.class, new Position(i, 0, 0),
                    Health.class, new Health(i % 100, 100)
            ));
        }

        System.out.println("\n=== Parallel Batch Processing Example ===");

        // ========== Process in parallel batches ==========
        int numThreads = 4;
        int batchSize = 250;  // 1000 / 4 threads

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(numThreads);
        java.util.concurrent.atomic.AtomicInteger totalDamaged = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            final int startIdx = t * batchSize;

            new Thread(() -> {
                try {
                    ComponentReader<Health> healthReader = archetype.getReader(Health.class);
                    int localDamaged = 0;

                    // Process this thread's batch
                    for (int i = startIdx; i < startIdx + batchSize; i++) {
                        Health health = healthReader.read(i);
                        if (health.current() < 50) {
                            localDamaged++;
                        }
                    }

                    totalDamaged.addAndGet(localDamaged);
                    System.out.printf("Thread %d processed indices %d-%d, found %d low-health entities%n",
                            threadId, startIdx, startIdx + batchSize - 1, localDamaged);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        System.out.printf("Total low-health entities found: %d%n", totalDamaged.get());

        // Each batch of 250 has entities with health 0-99 repeating
        // Entities with health < 50 are those with i % 100 < 50
        // In 250 entities: indices 0-49 (50), 100-149 (50) = 100 per batch? No wait...
        // Health is i % 100, so in range [0, 249]: health values are 0-99, 0-99, 0-49
        // That's 50 + 50 + 50 = 150 entities with health < 50 per 250
        // Actually: in indices 0-249, health = i % 100
        // i=0-49: health 0-49 (50 < 50)
        // i=50-99: health 50-99 (0 < 50)
        // i=100-149: health 0-49 (50 < 50)
        // i=150-199: health 50-99 (0 < 50)
        // i=200-249: health 0-49 (50 < 50)
        // Per 250: 50 + 0 + 50 + 0 + 50 = 150? No...
        // Let me recalculate: for each 100 indices, 50 have health < 50
        // So 1000 entities -> 500 with health < 50
        assertEquals(500, totalDamaged.get());
    }
}
