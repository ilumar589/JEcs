package io.github.ilumar589.jecs.system;

import io.github.ilumar589.jecs.component.Health;
import io.github.ilumar589.jecs.component.Position;
import io.github.ilumar589.jecs.component.Velocity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ComponentAccess conflict detection.
 */
class ComponentAccessTest {

    // ==================== No Conflict Tests ====================

    @Test
    void noConflictWhenReadingDifferentComponents() {
        ComponentAccess access1 = new ComponentAccess.Builder()
                .addReadOnly(Position.class)
                .build();

        ComponentAccess access2 = new ComponentAccess.Builder()
                .addReadOnly(Velocity.class)
                .build();

        assertFalse(access1.conflictsWith(access2));
        assertFalse(access2.conflictsWith(access1));
    }

    @Test
    void noConflictWhenReadingSameComponents() {
        ComponentAccess access1 = new ComponentAccess.Builder()
                .addReadOnly(Position.class)
                .build();

        ComponentAccess access2 = new ComponentAccess.Builder()
                .addReadOnly(Position.class)
                .build();

        assertFalse(access1.conflictsWith(access2));
        assertFalse(access2.conflictsWith(access1));
    }

    @Test
    void noConflictWhenMutatingDifferentComponents() {
        ComponentAccess access1 = new ComponentAccess.Builder()
                .addMutable(Position.class)
                .build();

        ComponentAccess access2 = new ComponentAccess.Builder()
                .addMutable(Velocity.class)
                .build();

        assertFalse(access1.conflictsWith(access2));
        assertFalse(access2.conflictsWith(access1));
    }

    @Test
    void noConflictWhenEmptyAccess() {
        ComponentAccess access1 = new ComponentAccess.Builder().build();
        ComponentAccess access2 = new ComponentAccess.Builder().build();

        assertFalse(access1.conflictsWith(access2));
    }

    // ==================== Conflict Tests ====================

    @Test
    void conflictWhenBothMutateSameComponent() {
        ComponentAccess access1 = new ComponentAccess.Builder()
                .addMutable(Position.class)
                .build();

        ComponentAccess access2 = new ComponentAccess.Builder()
                .addMutable(Position.class)
                .build();

        assertTrue(access1.conflictsWith(access2));
        assertTrue(access2.conflictsWith(access1));
    }

    @Test
    void conflictWhenOneMutatesWhatOtherReads() {
        ComponentAccess access1 = new ComponentAccess.Builder()
                .addMutable(Position.class)
                .build();

        ComponentAccess access2 = new ComponentAccess.Builder()
                .addReadOnly(Position.class)
                .build();

        assertTrue(access1.conflictsWith(access2));
        assertTrue(access2.conflictsWith(access1));
    }

    @Test
    void complexConflictScenario() {
        // Physics: reads Velocity, writes Position
        ComponentAccess physics = new ComponentAccess.Builder()
                .addReadOnly(Velocity.class)
                .addMutable(Position.class)
                .build();

        // Render: reads Position (conflict with physics writing Position)
        ComponentAccess render = new ComponentAccess.Builder()
                .addReadOnly(Position.class)
                .build();

        // AI: reads Health, writes Velocity (no conflict with physics or render)
        ComponentAccess ai = new ComponentAccess.Builder()
                .addReadOnly(Health.class)
                .addMutable(Velocity.class)
                .build();

        // Physics and Render conflict (physics writes Position, render reads it)
        assertTrue(physics.conflictsWith(render));
        assertTrue(render.conflictsWith(physics));

        // Physics and AI conflict (physics reads Velocity, AI writes it)
        assertTrue(physics.conflictsWith(ai));
        assertTrue(ai.conflictsWith(physics));

        // Render and AI don't conflict
        assertFalse(render.conflictsWith(ai));
        assertFalse(ai.conflictsWith(render));
    }

    // ==================== Builder Tests ====================

    @Test
    void builderAccumulatesReadOnly() {
        ComponentAccess access = new ComponentAccess.Builder()
                .addReadOnly(Position.class)
                .addReadOnly(Velocity.class)
                .build();

        assertTrue(access.getReadOnly().contains(Position.class));
        assertTrue(access.getReadOnly().contains(Velocity.class));
        assertEquals(2, access.getReadOnly().size());
    }

    @Test
    void builderAccumulatesMutable() {
        ComponentAccess access = new ComponentAccess.Builder()
                .addMutable(Position.class)
                .addMutable(Velocity.class)
                .build();

        assertTrue(access.getMutable().contains(Position.class));
        assertTrue(access.getMutable().contains(Velocity.class));
        assertEquals(2, access.getMutable().size());
    }

    @Test
    void builderAccumulatesExcluded() {
        ComponentAccess access = new ComponentAccess.Builder()
                .addExcluded(Health.class)
                .build();

        assertTrue(access.getExcluded().contains(Health.class));
        assertEquals(1, access.getExcluded().size());
    }

    @Test
    void builderVarargs() {
        ComponentAccess access = new ComponentAccess.Builder()
                .addReadOnly(Position.class, Velocity.class)
                .addMutable(Health.class)
                .addExcluded(Position.class, Velocity.class)
                .build();

        assertEquals(2, access.getReadOnly().size());
        assertEquals(1, access.getMutable().size());
        assertEquals(2, access.getExcluded().size());
    }

    // ==================== Accessor Tests ====================

    @Test
    void getAllAccessedReturnsReadOnlyAndMutable() {
        ComponentAccess access = new ComponentAccess.Builder()
                .addReadOnly(Position.class)
                .addMutable(Velocity.class)
                .build();

        var all = access.getAllAccessed();
        assertTrue(all.contains(Position.class));
        assertTrue(all.contains(Velocity.class));
        assertEquals(2, all.size());
    }

    @Test
    void hasReadOnlyReturnsTrueWhenHasReadOnly() {
        ComponentAccess access = new ComponentAccess.Builder()
                .addReadOnly(Position.class)
                .build();

        assertTrue(access.hasReadOnly());
        assertFalse(access.hasMutable());
        assertFalse(access.hasExcluded());
    }

    @Test
    void hasMutableReturnsTrueWhenHasMutable() {
        ComponentAccess access = new ComponentAccess.Builder()
                .addMutable(Position.class)
                .build();

        assertFalse(access.hasReadOnly());
        assertTrue(access.hasMutable());
        assertFalse(access.hasExcluded());
    }

    @Test
    void hasExcludedReturnsTrueWhenHasExcluded() {
        ComponentAccess access = new ComponentAccess.Builder()
                .addExcluded(Health.class)
                .build();

        assertFalse(access.hasReadOnly());
        assertFalse(access.hasMutable());
        assertTrue(access.hasExcluded());
    }

    // ==================== Immutability Tests ====================

    @Test
    void returnedSetsAreImmutable() {
        ComponentAccess access = new ComponentAccess.Builder()
                .addReadOnly(Position.class)
                .addMutable(Velocity.class)
                .addExcluded(Health.class)
                .build();

        assertThrows(UnsupportedOperationException.class, () -> access.getReadOnly().add(Health.class));
        assertThrows(UnsupportedOperationException.class, () -> access.getMutable().add(Health.class));
        assertThrows(UnsupportedOperationException.class, () -> access.getExcluded().add(Position.class));
        assertThrows(UnsupportedOperationException.class, () -> access.getAllAccessed().add(Health.class));
    }
}
