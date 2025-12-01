package io.github.ilumar589.jecs.component;

/**
 * A marker component representing that an entity is dead.
 * Used for filtering out dead entities from queries.
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Query only living entities (exclude Dead)
 * world.componentQuery()
 *     .with(Health.class)
 *     .without(Dead.class)
 *     .forEach(health -> {
 *         // Process only living entities
 *     });
 * }</pre>
 * 
 * <h2>Marker Component Pattern</h2>
 * Dead has a dummy field because the ECS requires records with at least
 * one primitive field for storage. The value is always true.
 *
 * @param isDead always true for this marker
 */
public record Dead(boolean isDead) {
    /**
     * Creates a Dead marker component with default value.
     */
    public Dead() {
        this(true);
    }
}
