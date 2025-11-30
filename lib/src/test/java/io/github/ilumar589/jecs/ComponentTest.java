package io.github.ilumar589.jecs;

import io.github.ilumar589.jecs.component.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentTest {

    @Test
    void positionRecordProperties() {
        Position position = new Position(1.0f, 2.0f, 3.0f);

        assertEquals(1.0f, position.x());
        assertEquals(2.0f, position.y());
        assertEquals(3.0f, position.z());
    }

    @Test
    void positionIsRecord() {
        Position position = new Position(1.0f, 2.0f, 3.0f);
        assertInstanceOf(Record.class, position);
    }

    @Test
    void positionEquality() {
        Position p1 = new Position(1.0f, 2.0f, 3.0f);
        Position p2 = new Position(1.0f, 2.0f, 3.0f);
        Position p3 = new Position(4.0f, 5.0f, 6.0f);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1, p3);
    }

    @Test
    void velocityRecordProperties() {
        Velocity velocity = new Velocity(0.1f, 0.2f, 0.3f);

        assertEquals(0.1f, velocity.dx());
        assertEquals(0.2f, velocity.dy());
        assertEquals(0.3f, velocity.dz());
    }

    @Test
    void velocityIsRecord() {
        Velocity velocity = new Velocity(0.1f, 0.2f, 0.3f);
        assertInstanceOf(Record.class, velocity);
    }

    @Test
    void velocityEquality() {
        Velocity v1 = new Velocity(0.1f, 0.2f, 0.3f);
        Velocity v2 = new Velocity(0.1f, 0.2f, 0.3f);
        Velocity v3 = new Velocity(0.4f, 0.5f, 0.6f);

        assertEquals(v1, v2);
        assertEquals(v1.hashCode(), v2.hashCode());
        assertNotEquals(v1, v3);
    }

    @Test
    void healthRecordProperties() {
        Health health = new Health(80, 100);

        assertEquals(80, health.current());
        assertEquals(100, health.max());
    }

    @Test
    void healthIsRecord() {
        Health health = new Health(80, 100);
        assertInstanceOf(Record.class, health);
    }

    @Test
    void healthEquality() {
        Health h1 = new Health(80, 100);
        Health h2 = new Health(80, 100);
        Health h3 = new Health(50, 100);

        assertEquals(h1, h2);
        assertEquals(h1.hashCode(), h2.hashCode());
        assertNotEquals(h1, h3);
    }
}
