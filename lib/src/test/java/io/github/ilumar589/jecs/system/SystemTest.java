package io.github.ilumar589.jecs.system;

import io.github.ilumar589.jecs.component.Dead;
import io.github.ilumar589.jecs.component.Health;
import io.github.ilumar589.jecs.component.Position;
import io.github.ilumar589.jecs.component.Velocity;
import io.github.ilumar589.jecs.query.Mutable;
import io.github.ilumar589.jecs.query.ReadOnly;
import io.github.ilumar589.jecs.world.EcsWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the System class.
 */
class SystemTest {

    private EcsWorld world;

    @BeforeEach
    void setUp() {
        world = new EcsWorld();
    }

    // ==================== Builder Tests ====================

    @Test
    void builderRequiresName() {
        assertThrows(IllegalArgumentException.class, () -> new System.Builder(null));
        assertThrows(IllegalArgumentException.class, () -> new System.Builder(""));
    }

    @Test
    void builderRequiresExecutor() {
        assertThrows(IllegalStateException.class, () ->
                new System.Builder("Test")
                        .withReadOnly(Position.class)
                        .build()
        );
    }

    @Test
    void builderCreatesSystemWithName() {
        System system = new System.Builder("Physics")
                .withMutable(Position.class)
                .execute((w, q) -> {})
                .build();

        assertEquals("Physics", system.getName());
    }

    @Test
    void builderCreatesSystemWithReadOnlyAccess() {
        System system = new System.Builder("Render")
                .withReadOnly(Position.class)
                .withReadOnly(Velocity.class)
                .execute((w, q) -> {})
                .build();

        assertTrue(system.getAccess().getReadOnly().contains(Position.class));
        assertTrue(system.getAccess().getReadOnly().contains(Velocity.class));
        assertFalse(system.getAccess().hasMutable());
    }

    @Test
    void builderCreatesSystemWithMutableAccess() {
        System system = new System.Builder("Physics")
                .withMutable(Position.class)
                .execute((w, q) -> {})
                .build();

        assertTrue(system.getAccess().getMutable().contains(Position.class));
        assertFalse(system.getAccess().hasReadOnly());
    }

    @Test
    void builderCreatesSystemWithExclusion() {
        System system = new System.Builder("LivingHealth")
                .withMutable(Health.class)
                .without(Dead.class)
                .execute((w, q) -> {})
                .build();

        assertTrue(system.getAccess().getExcluded().contains(Dead.class));
    }

    @Test
    void builderCreatesSystemWithVarargs() {
        System system = new System.Builder("Complex")
                .withReadOnly(Position.class, Velocity.class)
                .withMutable(Health.class)
                .without(Dead.class)
                .execute((w, q) -> {})
                .build();

        assertEquals(2, system.getAccess().getReadOnly().size());
        assertEquals(1, system.getAccess().getMutable().size());
        assertEquals(1, system.getAccess().getExcluded().size());
    }

    // ==================== Execution Tests ====================

    @Test
    void systemExecutesOnWorld() {
        world.spawn(new Position(1, 0, 0), new Velocity(1, 0, 0));
        world.spawn(new Position(2, 0, 0), new Velocity(2, 0, 0));

        AtomicInteger count = new AtomicInteger(0);

        System system = new System.Builder("Counter")
                .withReadOnly(Position.class)
                .withReadOnly(Velocity.class)
                .execute((w, q) -> {
                    q.forEach(Position.class, Velocity.class, (pos, vel) -> {
                        count.incrementAndGet();
                    });
                })
                .build();

        system.execute(world);

        assertEquals(2, count.get());
    }

    @Test
    void systemModifiesComponents() {
        world.spawn(new Position(0, 0, 0), new Velocity(1, 2, 3));

        System physics = new System.Builder("Physics")
                .withMutable(Position.class)
                .withReadOnly(Velocity.class)
                .execute((w, q) -> {
                    q.forEach(wrappers -> {
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
                })
                .build();

        physics.execute(world);

        // Verify position was updated
        AtomicInteger verified = new AtomicInteger(0);
        world.componentQuery()
                .forEach(Position.class, pos -> {
                    assertEquals(1.0f, pos.x());
                    assertEquals(2.0f, pos.y());
                    assertEquals(3.0f, pos.z());
                    verified.incrementAndGet();
                });
        assertEquals(1, verified.get());
    }

    @Test
    void systemRespectsExclusion() {
        world.spawn(new Health(100, 100));
        world.spawn(new Health(0, 100), new Dead());

        AtomicInteger count = new AtomicInteger(0);

        System livingHealth = new System.Builder("LivingHealth")
                .withReadOnly(Health.class)
                .without(Dead.class)
                .execute((w, q) -> {
                    q.forEach(Health.class, health -> {
                        count.incrementAndGet();
                    });
                })
                .build();

        livingHealth.execute(world);

        assertEquals(1, count.get()); // Only living entity
    }

    // ==================== Conflict Detection Tests ====================

    @Test
    void systemsConflictWhenWritingSameComponent() {
        System system1 = new System.Builder("S1")
                .withMutable(Position.class)
                .execute((w, q) -> {})
                .build();

        System system2 = new System.Builder("S2")
                .withMutable(Position.class)
                .execute((w, q) -> {})
                .build();

        assertTrue(system1.conflictsWith(system2));
        assertTrue(system2.conflictsWith(system1));
    }

    @Test
    void systemsConflictWhenOneWritesWhatOtherReads() {
        System writer = new System.Builder("Writer")
                .withMutable(Position.class)
                .execute((w, q) -> {})
                .build();

        System reader = new System.Builder("Reader")
                .withReadOnly(Position.class)
                .execute((w, q) -> {})
                .build();

        assertTrue(writer.conflictsWith(reader));
        assertTrue(reader.conflictsWith(writer));
    }

    @Test
    void systemsDontConflictWhenReadingSameComponent() {
        System reader1 = new System.Builder("Reader1")
                .withReadOnly(Position.class)
                .execute((w, q) -> {})
                .build();

        System reader2 = new System.Builder("Reader2")
                .withReadOnly(Position.class)
                .execute((w, q) -> {})
                .build();

        assertFalse(reader1.conflictsWith(reader2));
        assertFalse(reader2.conflictsWith(reader1));
    }

    @Test
    void systemsDontConflictWhenAccessingDifferentComponents() {
        System system1 = new System.Builder("S1")
                .withMutable(Position.class)
                .execute((w, q) -> {})
                .build();

        System system2 = new System.Builder("S2")
                .withMutable(Velocity.class)
                .execute((w, q) -> {})
                .build();

        assertFalse(system1.conflictsWith(system2));
        assertFalse(system2.conflictsWith(system1));
    }

    // ==================== Equality Tests ====================

    @Test
    void systemsWithSameNameAreEqual() {
        System system1 = new System.Builder("Physics")
                .withMutable(Position.class)
                .execute((w, q) -> {})
                .build();

        System system2 = new System.Builder("Physics")
                .withReadOnly(Velocity.class)
                .execute((w, q) -> {})
                .build();

        assertEquals(system1, system2);
        assertEquals(system1.hashCode(), system2.hashCode());
    }

    @Test
    void systemsWithDifferentNamesAreNotEqual() {
        System system1 = new System.Builder("Physics")
                .execute((w, q) -> {})
                .build();

        System system2 = new System.Builder("Render")
                .execute((w, q) -> {})
                .build();

        assertNotEquals(system1, system2);
    }

    // ==================== ToString Tests ====================

    @Test
    void toStringIncludesNameAndAccess() {
        System system = new System.Builder("Physics")
                .withMutable(Position.class)
                .execute((w, q) -> {})
                .build();

        String str = system.toString();
        assertTrue(str.contains("Physics"));
        assertTrue(str.contains("mutable="));
    }

    // ==================== SystemMode Tests ====================

    @Test
    void defaultModeIsUpdate() {
        System system = new System.Builder("Test")
                .execute((w, q) -> {})
                .build();

        assertEquals(SystemMode.UPDATE, system.getMode());
    }

    @Test
    void startupModeCanBeSet() {
        System system = new System.Builder("Startup")
                .mode(SystemMode.STARTUP)
                .execute((w, q) -> {})
                .build();

        assertEquals(SystemMode.STARTUP, system.getMode());
    }

    @Test
    void updateModeCanBeSet() {
        System system = new System.Builder("Update")
                .mode(SystemMode.UPDATE)
                .execute((w, q) -> {})
                .build();

        assertEquals(SystemMode.UPDATE, system.getMode());
    }

    @Test
    void shutdownModeCanBeSet() {
        System system = new System.Builder("Shutdown")
                .mode(SystemMode.SHUTDOWN)
                .execute((w, q) -> {})
                .build();

        assertEquals(SystemMode.SHUTDOWN, system.getMode());
    }

    @Test
    void modeCanBeCombinedWithOtherBuilderMethods() {
        System system = new System.Builder("Complex")
                .mode(SystemMode.STARTUP)
                .withMutable(Position.class)
                .withReadOnly(Velocity.class)
                .without(Dead.class)
                .execute((w, q) -> {})
                .build();

        assertEquals(SystemMode.STARTUP, system.getMode());
        assertTrue(system.getAccess().getMutable().contains(Position.class));
        assertTrue(system.getAccess().getReadOnly().contains(Velocity.class));
        assertTrue(system.getAccess().getExcluded().contains(Dead.class));
    }

    @Test
    void modeCanBeChainedWithExecute() {
        System system = new System.Builder("Test")
                .withMutable(Position.class)
                .mode(SystemMode.SHUTDOWN)
                .execute((w, q) -> {})
                .build();

        assertEquals(SystemMode.SHUTDOWN, system.getMode());
        assertTrue(system.getAccess().getMutable().contains(Position.class));
    }
}
