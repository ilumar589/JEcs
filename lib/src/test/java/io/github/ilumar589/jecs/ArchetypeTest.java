package io.github.ilumar589.jecs;

import io.github.ilumar589.jecs.component.*;
import io.github.ilumar589.jecs.entity.Entity;
import io.github.ilumar589.jecs.world.Archetype;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ArchetypeTest {

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
    void getComponentColumnReturnsAllComponents() {
        Set<Class<?>> types = Set.of(Position.class);
        Archetype archetype = new Archetype(types);

        Position p1 = new Position(1, 0, 0);
        Position p2 = new Position(2, 0, 0);

        archetype.addEntity(new Entity(1), Map.of(Position.class, p1));
        archetype.addEntity(new Entity(2), Map.of(Position.class, p2));

        List<Position> positions = archetype.getComponentColumn(Position.class);
        assertEquals(2, positions.size());
        assertTrue(positions.contains(p1));
        assertTrue(positions.contains(p2));
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
}
