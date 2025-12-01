package io.github.ilumar589.jecs;

import io.github.ilumar589.jecs.component.*;
import io.github.ilumar589.jecs.entity.Entity;
import io.github.ilumar589.jecs.world.primitive.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PrimitiveArchetype comparing behavior with standard Archetype.
 */
class PrimitiveArchetypeTest {

    @Test
    void createArchetypeWithComponentTypes() {
        Set<Class<?>> types = Set.of(Position.class, Velocity.class);
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

        assertEquals(types, archetype.getComponentTypes());
        assertEquals(0, archetype.size());
    }

    @Test
    void addEntityToArchetype() {
        Set<Class<?>> types = Set.of(Position.class);
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

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
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

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
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

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
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

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
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

        Entity entity = new Entity(999);
        assertNull(archetype.getComponent(entity, Position.class));
    }

    @Test
    void getComponentReturnsNullForNonexistentType() {
        Set<Class<?>> types = Set.of(Position.class);
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

        Entity entity = new Entity(1);
        archetype.addEntity(entity, Map.of(Position.class, new Position(1, 2, 3)));

        assertNull(archetype.getComponent(entity, Velocity.class));
    }

    @Test
    void setComponentUpdatesValue() {
        Set<Class<?>> types = Set.of(Position.class);
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

        Entity entity = new Entity(1);
        archetype.addEntity(entity, Map.of(Position.class, new Position(1, 2, 3)));

        Position newPosition = new Position(10, 20, 30);
        archetype.setComponent(entity, newPosition);

        assertEquals(newPosition, archetype.getComponent(entity, Position.class));
    }

    @Test
    void getEntitiesReturnsAllEntities() {
        Set<Class<?>> types = Set.of(Position.class);
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

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
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

        Entity entity = new Entity(1);
        archetype.addEntity(entity, Map.of(Position.class, new Position(1, 2, 3)));

        assertThrows(IllegalArgumentException.class, () -> 
                archetype.addEntity(entity, Map.of(Position.class, new Position(4, 5, 6))));
    }

    @Test
    void addEntityWithWrongComponentTypesThrowsException() {
        Set<Class<?>> types = Set.of(Position.class, Velocity.class);
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

        Entity entity = new Entity(1);

        assertThrows(IllegalArgumentException.class, () -> 
                archetype.addEntity(entity, Map.of(Position.class, new Position(1, 2, 3))));
    }

    @Test
    void removeNonexistentEntityThrowsException() {
        Set<Class<?>> types = Set.of(Position.class);
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

        assertThrows(IllegalArgumentException.class, () -> archetype.removeEntity(new Entity(999)));
    }

    @Test
    void archetypesWithSameTypesAreEqual() {
        Set<Class<?>> types = Set.of(Position.class, Velocity.class);
        PrimitiveArchetype a1 = new PrimitiveArchetype(types);
        PrimitiveArchetype a2 = new PrimitiveArchetype(types);

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    void archetypesWithDifferentTypesAreNotEqual() {
        PrimitiveArchetype a1 = new PrimitiveArchetype(Set.of(Position.class));
        PrimitiveArchetype a2 = new PrimitiveArchetype(Set.of(Velocity.class));

        assertNotEquals(a1, a2);
    }

    // --- ComponentReader tests ---

    @Test
    void componentReaderReadsCorrectValues() {
        Set<Class<?>> types = Set.of(Position.class);
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

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
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

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
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

        archetype.addEntity(new Entity(1), Map.of(Position.class, new Position(1, 2, 3)));

        ComponentReader<Position> reader = archetype.getReader(Position.class);

        assertThrows(IndexOutOfBoundsException.class, () -> reader.read(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> reader.read(1));
        assertThrows(IndexOutOfBoundsException.class, () -> reader.read(100));
    }

    @Test
    void componentReaderForNonexistentTypeThrows() {
        Set<Class<?>> types = Set.of(Position.class);
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

        assertThrows(IllegalArgumentException.class, () -> archetype.getReader(Velocity.class));
    }

    // --- ComponentWriter tests ---

    @Test
    void componentWriterWritesCorrectValues() {
        Set<Class<?>> types = Set.of(Position.class);
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

        archetype.addEntity(new Entity(1), Map.of(Position.class, new Position(1, 2, 3)));

        ComponentWriter<Position> writer = archetype.getWriter(Position.class);
        Position newPos = new Position(10, 20, 30);
        writer.write(0, newPos);

        assertEquals(newPos, archetype.getComponent(new Entity(1), Position.class));
    }

    @Test
    void componentWriterSize() {
        Set<Class<?>> types = Set.of(Position.class);
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

        archetype.addEntity(new Entity(1), Map.of(Position.class, new Position(1, 2, 3)));
        archetype.addEntity(new Entity(2), Map.of(Position.class, new Position(4, 5, 6)));

        ComponentWriter<Position> writer = archetype.getWriter(Position.class);
        assertEquals(2, writer.size());
    }

    @Test
    void componentWriterThrowsOnInvalidIndex() {
        Set<Class<?>> types = Set.of(Position.class);
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

        archetype.addEntity(new Entity(1), Map.of(Position.class, new Position(1, 2, 3)));

        ComponentWriter<Position> writer = archetype.getWriter(Position.class);

        assertThrows(IndexOutOfBoundsException.class, () -> writer.write(-1, new Position(0, 0, 0)));
        assertThrows(IndexOutOfBoundsException.class, () -> writer.write(1, new Position(0, 0, 0)));
    }

    @Test
    void componentWriterForNonexistentTypeThrows() {
        Set<Class<?>> types = Set.of(Position.class);
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

        assertThrows(IllegalArgumentException.class, () -> archetype.getWriter(Velocity.class));
    }

    // --- Multiple component tests ---

    @Test
    void multipleComponentTypesStoredCorrectly() {
        Set<Class<?>> types = Set.of(Position.class, Velocity.class, Health.class);
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

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
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

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
        PrimitiveArchetype archetype = new PrimitiveArchetype(types, 4);

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
        PrimitiveArchetype archetype = new PrimitiveArchetype(types, 100);

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
        class NonRecordComponent {
            int value;
        }

        assertThrows(IllegalArgumentException.class, () -> 
                new PrimitiveArchetype(Set.of(NonRecordComponent.class)));
    }

    // --- PrimitiveType tests ---

    @Test
    void primitiveTypeFromClass() {
        assertEquals(PrimitiveType.INT, PrimitiveType.fromClass(int.class));
        assertEquals(PrimitiveType.INT, PrimitiveType.fromClass(Integer.class));
        assertEquals(PrimitiveType.FLOAT, PrimitiveType.fromClass(float.class));
        assertEquals(PrimitiveType.FLOAT, PrimitiveType.fromClass(Float.class));
        assertEquals(PrimitiveType.DOUBLE, PrimitiveType.fromClass(double.class));
        assertEquals(PrimitiveType.LONG, PrimitiveType.fromClass(long.class));
        assertEquals(PrimitiveType.BOOLEAN, PrimitiveType.fromClass(boolean.class));
        assertEquals(PrimitiveType.BYTE, PrimitiveType.fromClass(byte.class));
        assertEquals(PrimitiveType.SHORT, PrimitiveType.fromClass(short.class));
        assertEquals(PrimitiveType.CHAR, PrimitiveType.fromClass(char.class));
        assertEquals(PrimitiveType.STRING, PrimitiveType.fromClass(String.class));
        assertNull(PrimitiveType.fromClass(Object.class));
    }

    @Test
    void primitiveTypeCreateArray() {
        assertInstanceOf(int[].class, PrimitiveType.INT.createArray(10));
        assertInstanceOf(float[].class, PrimitiveType.FLOAT.createArray(10));
        assertInstanceOf(double[].class, PrimitiveType.DOUBLE.createArray(10));
        assertInstanceOf(long[].class, PrimitiveType.LONG.createArray(10));
        assertInstanceOf(boolean[].class, PrimitiveType.BOOLEAN.createArray(10));
        assertInstanceOf(String[].class, PrimitiveType.STRING.createArray(10));
    }

    // --- FieldKey tests ---

    @Test
    void fieldKeyEquality() {
        FieldKey key1 = new FieldKey(Position.class, "x", PrimitiveType.FLOAT);
        FieldKey key2 = new FieldKey(Position.class, "x", PrimitiveType.FLOAT);
        FieldKey key3 = new FieldKey(Velocity.class, "x", PrimitiveType.FLOAT);

        assertEquals(key1, key2);
        assertNotEquals(key1, key3);  // Different component type
    }

    @Test
    void fieldKeyValidation() {
        assertThrows(IllegalArgumentException.class, () -> 
                new FieldKey(null, "x", PrimitiveType.FLOAT));
        assertThrows(IllegalArgumentException.class, () -> 
                new FieldKey(Position.class, null, PrimitiveType.FLOAT));
        assertThrows(IllegalArgumentException.class, () -> 
                new FieldKey(Position.class, "", PrimitiveType.FLOAT));
        assertThrows(IllegalArgumentException.class, () -> 
                new FieldKey(Position.class, "x", null));
    }

    // --- ReadAll test ---

    @Test
    void componentReaderReadAll() {
        Set<Class<?>> types = Set.of(Position.class);
        PrimitiveArchetype archetype = new PrimitiveArchetype(types);

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
}
