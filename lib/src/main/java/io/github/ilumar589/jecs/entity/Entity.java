package io.github.ilumar589.jecs.entity;

/**
 * Represents an entity in the ECS world.
 * An entity is essentially just an identifier (ID) with an optional generation field for future safety.
 *
 * @param id the unique identifier for this entity
 * @param generation the generation/version number, incremented when an ID is reused
 */
public record Entity(int id, int generation) {
    /**
     * Creates a new entity with generation 0.
     *
     * @param id the unique identifier for this entity
     */
    public Entity(int id) {
        this(id, 0);
    }
}
