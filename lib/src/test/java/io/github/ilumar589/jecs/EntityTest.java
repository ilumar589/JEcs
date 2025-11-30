package io.github.ilumar589.jecs;

import io.github.ilumar589.jecs.entity.Entity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    @Test
    void createEntityWithId() {
        Entity entity = new Entity(42);
        assertEquals(42, entity.id());
        assertEquals(0, entity.generation());
    }

    @Test
    void createEntityWithIdAndGeneration() {
        Entity entity = new Entity(42, 5);
        assertEquals(42, entity.id());
        assertEquals(5, entity.generation());
    }

    @Test
    void entitiesWithSameIdAndGenerationAreEqual() {
        Entity e1 = new Entity(1, 0);
        Entity e2 = new Entity(1, 0);

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    void entitiesWithDifferentIdsAreNotEqual() {
        Entity e1 = new Entity(1);
        Entity e2 = new Entity(2);

        assertNotEquals(e1, e2);
    }

    @Test
    void entitiesWithDifferentGenerationsAreNotEqual() {
        Entity e1 = new Entity(1, 0);
        Entity e2 = new Entity(1, 1);

        assertNotEquals(e1, e2);
    }

    @Test
    void entityToString() {
        Entity entity = new Entity(42, 3);
        String str = entity.toString();
        assertTrue(str.contains("42"));
        assertTrue(str.contains("3"));
    }
}
