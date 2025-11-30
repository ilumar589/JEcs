package io.github.ilumar589.jecs;

import io.github.ilumar589.jecs.component.*;
import io.github.ilumar589.jecs.entity.Entity;
import io.github.ilumar589.jecs.world.EcsWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EcsWorldTest {

    private EcsWorld world;

    @BeforeEach
    void setUp() {
        world = new EcsWorld();
    }

    @Test
    void createEntityWithNoComponents() {
        Entity entity = world.createEntity();
        assertNotNull(entity);
        assertEquals(0, entity.id());
    }

    @Test
    void createEntityWithComponents() {
        Position position = new Position(1.0f, 2.0f, 3.0f);
        Velocity velocity = new Velocity(0.1f, 0.2f, 0.3f);

        Entity entity = world.createEntity(position, velocity);

        assertNotNull(entity);
        assertEquals(position, world.getComponent(entity, Position.class));
        assertEquals(velocity, world.getComponent(entity, Velocity.class));
    }

    @Test
    void createMultipleEntities() {
        Entity e1 = world.createEntity(new Position(1, 0, 0));
        Entity e2 = world.createEntity(new Position(2, 0, 0));
        Entity e3 = world.createEntity(new Position(3, 0, 0));

        assertNotEquals(e1.id(), e2.id());
        assertNotEquals(e2.id(), e3.id());
        assertEquals(3, world.getEntityCount());
    }

    @Test
    void destroyEntity() {
        Entity entity = world.createEntity(new Position(1, 2, 3));
        assertTrue(world.hasEntity(entity));

        world.destroyEntity(entity);
        assertFalse(world.hasEntity(entity));
        assertNull(world.getComponent(entity, Position.class));
    }

    @Test
    void getComponentReturnsNullForNonexistentEntity() {
        Entity entity = new Entity(999);
        assertNull(world.getComponent(entity, Position.class));
    }

    @Test
    void getComponentReturnsNullForMissingComponent() {
        Entity entity = world.createEntity(new Position(1, 2, 3));
        assertNull(world.getComponent(entity, Velocity.class));
    }

    @Test
    void addComponentToEntity() {
        Entity entity = world.createEntity(new Position(1, 2, 3));

        world.addComponent(entity, new Velocity(0.1f, 0.2f, 0.3f));

        assertNotNull(world.getComponent(entity, Position.class));
        assertNotNull(world.getComponent(entity, Velocity.class));
    }

    @Test
    void addComponentToEntityWithNoComponents() {
        Entity entity = world.createEntity();

        world.addComponent(entity, new Position(1, 2, 3));

        assertEquals(new Position(1, 2, 3), world.getComponent(entity, Position.class));
    }

    @Test
    void addDuplicateComponentThrowsException() {
        Entity entity = world.createEntity(new Position(1, 2, 3));

        assertThrows(IllegalArgumentException.class, () -> 
                world.addComponent(entity, new Position(4, 5, 6)));
    }

    @Test
    void removeComponentFromEntity() {
        Entity entity = world.createEntity(new Position(1, 2, 3), new Velocity(0.1f, 0.2f, 0.3f));

        world.removeComponent(entity, Velocity.class);

        assertNotNull(world.getComponent(entity, Position.class));
        assertNull(world.getComponent(entity, Velocity.class));
    }

    @Test
    void removeLastComponentFromEntity() {
        Entity entity = world.createEntity(new Position(1, 2, 3));

        world.removeComponent(entity, Position.class);

        assertNull(world.getComponent(entity, Position.class));
    }

    @Test
    void removeMissingComponentThrowsException() {
        Entity entity = world.createEntity(new Position(1, 2, 3));

        assertThrows(IllegalArgumentException.class, () -> 
                world.removeComponent(entity, Velocity.class));
    }

    @Test
    void setComponentUpdatesValue() {
        Entity entity = world.createEntity(new Position(1, 2, 3));

        world.setComponent(entity, new Position(10, 20, 30));

        assertEquals(new Position(10, 20, 30), world.getComponent(entity, Position.class));
    }

    @Test
    void queryEntitiesWithSingleComponent() {
        Entity e1 = world.createEntity(new Position(1, 0, 0));
        Entity e2 = world.createEntity(new Position(2, 0, 0), new Velocity(1, 0, 0));
        Entity e3 = world.createEntity(new Velocity(2, 0, 0));

        List<Entity> result = world.query(Position.class);

        assertEquals(2, result.size());
        assertTrue(result.contains(e1));
        assertTrue(result.contains(e2));
        assertFalse(result.contains(e3));
    }

    @Test
    void queryEntitiesWithMultipleComponents() {
        Entity e1 = world.createEntity(new Position(1, 0, 0));
        Entity e2 = world.createEntity(new Position(2, 0, 0), new Velocity(1, 0, 0));
        Entity e3 = world.createEntity(new Position(3, 0, 0), new Velocity(2, 0, 0), new Health(100, 100));

        List<Entity> result = world.query(Position.class, Velocity.class);

        assertEquals(2, result.size());
        assertTrue(result.contains(e2));
        assertTrue(result.contains(e3));
        assertFalse(result.contains(e1));
    }

    @Test
    void forEachIteratesOverMatchingEntities() {
        world.createEntity(new Position(1, 0, 0));
        world.createEntity(new Position(2, 0, 0), new Velocity(1, 0, 0));
        world.createEntity(new Velocity(2, 0, 0));

        int[] count = {0};
        world.forEach(entity -> count[0]++, Position.class);

        assertEquals(2, count[0]);
    }

    @Test
    void hasComponentReturnsCorrectly() {
        Entity entity = world.createEntity(new Position(1, 2, 3));

        assertTrue(world.hasComponent(entity, Position.class));
        assertFalse(world.hasComponent(entity, Velocity.class));
    }

    @Test
    void entitiesSharingArchetypeAreGroupedTogether() {
        world.createEntity(new Position(1, 0, 0), new Velocity(1, 0, 0));
        world.createEntity(new Position(2, 0, 0), new Velocity(2, 0, 0));
        world.createEntity(new Position(3, 0, 0)); // Different archetype

        // Two archetypes: (Position, Velocity) and (Position)
        assertEquals(2, world.getArchetypeCount());
    }

    @Test
    void archetypeCountIncreasesWhenAddingComponent() {
        Entity entity = world.createEntity(new Position(1, 2, 3));
        int initialCount = world.getArchetypeCount();

        world.addComponent(entity, new Velocity(0.1f, 0.2f, 0.3f));

        // Should have both (Position) and (Position, Velocity) archetypes
        assertEquals(initialCount + 1, world.getArchetypeCount());
    }

    @Test
    void createEntityWithDuplicateComponentTypesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                world.createEntity(new Position(1, 2, 3), new Position(4, 5, 6)));
    }
}
