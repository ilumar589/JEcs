package io.github.ilumar589.jecs.world;

import io.github.ilumar589.jecs.entity.Entity;
import io.github.ilumar589.jecs.query.ComponentQuery;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The main entry point for the ECS framework.
 * Manages entities, components, and archetypes.
 * 
 * <h2>Project Valhalla Readiness</h2>
 * This ECS implementation is designed to be fully compatible with Project Valhalla:
 * <ul>
 *   <li>No marker interface required - components are just plain records/classes</li>
 *   <li>Value types can be stored flattened without interface overhead</li>
 *   <li>When Valhalla lands, records can become value types with zero code changes</li>
 * </ul>
 * 
 * <h2>Design Decision: Object Parameters vs Type Bounds</h2>
 * This API uses {@code Object} parameters instead of bounded generics like
 * {@code T extends Component}. This is an intentional trade-off:
 * <ul>
 *   <li><b>Why:</b> Marker interfaces prevent value type flattening in Valhalla.
 *       Value types implementing interfaces require object references, losing
 *       cache locality and inline storage benefits.</li>
 *   <li><b>Trade-off:</b> Compile-time type safety is reduced - any object can be
 *       passed as a component. Use records/value types for components.</li>
 *   <li><b>Best Practice:</b> Components should be immutable records with primitive
 *       fields (e.g., {@code record Position(float x, float y, float z)}).</li>
 * </ul>
 */
public final class EcsWorld {
    private int nextEntityId = 0;
    private final Map<Entity, Archetype> entityToArchetype;
    private final Map<ArchetypeKey, Archetype> archetypes;

    /**
     * Creates a new empty ECS world.
     */
    public EcsWorld() {
        this.entityToArchetype = new HashMap<>();
        this.archetypes = new HashMap<>();
    }

    /**
     * Creates a new entity with the specified components.
     * 
     * <p>Components should be immutable records with primitive fields for best
     * performance and Valhalla compatibility. Each component type should be unique
     * per entity.</p>
     *
     * @param components the initial components for the entity (should be records)
     * @return the newly created entity
     * @throws IllegalArgumentException if duplicate component types are provided
     */
    public Entity createEntity(Object... components) {
        Entity entity = new Entity(nextEntityId++);

        if (components.length == 0) {
            return entity;
        }

        Set<Class<?>> typeSet = new HashSet<>();
        Map<Class<?>, Object> componentMap = new HashMap<>();

        for (Object component : components) {
            Class<?> type = component.getClass();
            if (typeSet.contains(type)) {
                throw new IllegalArgumentException("Duplicate component type: " + type);
            }
            typeSet.add(type);
            componentMap.put(type, component);
        }

        Archetype archetype = getOrCreateArchetype(typeSet);
        archetype.addEntity(entity, componentMap);
        entityToArchetype.put(entity, archetype);

        return entity;
    }

    /**
     * Destroys an entity, removing it from the world.
     *
     * @param entity the entity to destroy
     */
    public void destroyEntity(Entity entity) {
        Archetype archetype = entityToArchetype.remove(entity);
        if (archetype != null) {
            archetype.removeEntity(entity);
        }
    }

    /**
     * Checks if an entity exists in this world.
     *
     * @param entity the entity to check
     * @return true if the entity exists
     */
    public boolean hasEntity(Entity entity) {
        return entityToArchetype.containsKey(entity);
    }

    /**
     * Gets a component from an entity.
     *
     * @param entity the entity
     * @param type the component type
     * @param <T> the component type
     * @return the component, or null if the entity doesn't have this component
     */
    public <T> @Nullable T getComponent(Entity entity, Class<T> type) {
        Archetype archetype = entityToArchetype.get(entity);
        if (archetype == null) {
            return null;
        }
        return archetype.getComponent(entity, type);
    }

    /**
     * Checks if an entity has a specific component.
     *
     * @param entity the entity
     * @param type the component type
     * @return true if the entity has the component
     */
    public boolean hasComponent(Entity entity, Class<?> type) {
        Archetype archetype = entityToArchetype.get(entity);
        if (archetype == null) {
            return false;
        }
        return archetype.getComponentTypes().contains(type);
    }

    /**
     * Adds a component to an entity.
     * This may move the entity to a different archetype.
     * 
     * <p>Components should be immutable records with primitive fields for best
     * performance and Valhalla compatibility.</p>
     *
     * @param entity the entity
     * @param component the component to add (should be a record)
     * @throws IllegalArgumentException if the entity doesn't exist or already has this component type
     */
    public void addComponent(Entity entity, Object component) {
        Archetype oldArchetype = entityToArchetype.get(entity);

        Set<Class<?>> newTypes = new HashSet<>();
        Map<Class<?>, Object> componentMap = new HashMap<>();

        Class<?> newType = component.getClass();

        if (oldArchetype != null) {
            if (oldArchetype.getComponentTypes().contains(newType)) {
                throw new IllegalArgumentException("Entity already has component of type: " + newType);
            }

            Map<Class<?>, Object> oldComponents = oldArchetype.removeEntity(entity);
            newTypes.addAll(oldArchetype.getComponentTypes());
            componentMap.putAll(oldComponents);
        }

        newTypes.add(newType);
        componentMap.put(newType, component);

        Archetype newArchetype = getOrCreateArchetype(newTypes);
        newArchetype.addEntity(entity, componentMap);
        entityToArchetype.put(entity, newArchetype);
    }

    /**
     * Removes a component from an entity.
     * This may move the entity to a different archetype.
     *
     * @param entity the entity
     * @param type the component type to remove
     * @throws IllegalArgumentException if the entity doesn't exist or doesn't have this component
     */
    public void removeComponent(Entity entity, Class<?> type) {
        Archetype oldArchetype = entityToArchetype.get(entity);

        if (oldArchetype == null) {
            throw new IllegalArgumentException("Entity doesn't exist: " + entity);
        }

        if (!oldArchetype.getComponentTypes().contains(type)) {
            throw new IllegalArgumentException("Entity doesn't have component of type: " + type);
        }

        Map<Class<?>, Object> oldComponents = oldArchetype.removeEntity(entity);
        oldComponents.remove(type);

        if (oldComponents.isEmpty()) {
            entityToArchetype.remove(entity);
        } else {
            Set<Class<?>> newTypes = new HashSet<>(oldComponents.keySet());
            Archetype newArchetype = getOrCreateArchetype(newTypes);
            newArchetype.addEntity(entity, oldComponents);
            entityToArchetype.put(entity, newArchetype);
        }
    }

    /**
     * Sets (updates) a component on an entity.
     * The entity must already have a component of this type.
     * 
     * <p>Components should be immutable records with primitive fields for best
     * performance and Valhalla compatibility.</p>
     *
     * @param entity the entity
     * @param component the new component value (should be a record)
     * @throws IllegalArgumentException if the entity doesn't have this component type
     */
    public void setComponent(Entity entity, Object component) {
        Archetype archetype = entityToArchetype.get(entity);
        if (archetype == null) {
            throw new IllegalArgumentException("Entity doesn't exist: " + entity);
        }
        archetype.setComponent(entity, component);
    }

    /**
     * Queries entities that have all the specified component types.
     *
     * @param types the required component types
     * @return a list of entities that have all the specified components
     */
    @SafeVarargs
    public final List<Entity> query(Class<?>... types) {
        Set<Class<?>> requiredTypes = Set.of(types);
        List<Entity> result = new ArrayList<>();

        for (Archetype archetype : archetypes.values()) {
            if (archetype.getComponentTypes().containsAll(requiredTypes)) {
                result.addAll(archetype.getEntities());
            }
        }

        return result;
    }

    /**
     * Iterates over all entities that have all the specified component types.
     *
     * @param consumer the consumer to call for each matching entity
     * @param types the required component types
     */
    @SafeVarargs
    public final void forEach(Consumer<Entity> consumer, Class<?>... types) {
        Set<Class<?>> requiredTypes = Set.of(types);

        for (Archetype archetype : archetypes.values()) {
            if (archetype.getComponentTypes().containsAll(requiredTypes)) {
                for (Entity entity : archetype.getEntities()) {
                    consumer.accept(entity);
                }
            }
        }
    }

    // ==================== New Component-Focused API ====================

    /**
     * Spawns a new entity with the specified components.
     * This is the preferred method for creating entities in the new component-focused API.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.spawn(new Position(0, 0, 0), new Velocity(1, 1, 1));
     * }</pre>
     *
     * @param components the components for the new entity
     * @throws IllegalArgumentException if duplicate component types are provided
     */
    public void spawn(Object... components) {
        createEntity(components);
    }

    /**
     * Spawns multiple entities with components created by the provided suppliers.
     * Each entity receives one component from each supplier.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * Random random = new Random();
     * world.spawnBatch(100,
     *     () -> new Position(random.nextFloat(), random.nextFloat(), random.nextFloat()),
     *     () -> new Velocity(1, 0, 0)
     * );
     * }</pre>
     *
     * @param count the number of entities to spawn
     * @param suppliers the component suppliers
     * @throws IllegalArgumentException if duplicate component types are provided
     */
    @SafeVarargs
    public final void spawnBatch(int count, Supplier<Object>... suppliers) {
        for (int i = 0; i < count; i++) {
            Object[] components = new Object[suppliers.length];
            for (int j = 0; j < suppliers.length; j++) {
                components[j] = suppliers[j].get();
            }
            createEntity(components);
        }
    }

    /**
     * Creates a new fluent query builder for component-focused queries.
     * Use this to query entities without directly working with Entity objects.
     *
     * <h2>Usage Examples</h2>
     * <pre>{@code
     * // Simple query with inclusion
     * world.componentQuery()
     *     .with(Position.class, Velocity.class)
     *     .forEach((pos, vel) -> {
     *         System.out.println("Pos: " + pos + ", Vel: " + vel);
     *     });
     *
     * // Query with exclusion
     * world.componentQuery()
     *     .with(Position.class, Health.class)
     *     .without(Dead.class)
     *     .forEach((pos, health) -> {
     *         // Process only living entities
     *     });
     *
     * // Count matching entities
     * int count = world.componentQuery()
     *     .with(Position.class)
     *     .without(Velocity.class)
     *     .count();
     * }</pre>
     *
     * @return a new ComponentQuery builder
     */
    public ComponentQuery componentQuery() {
        return new ComponentQuery(archetypes.values(), this::setComponent);
    }

    /**
     * Returns the number of entities in the world.
     *
     * @return the entity count
     */
    public int getEntityCount() {
        return entityToArchetype.size();
    }

    /**
     * Returns the number of archetypes in the world.
     *
     * @return the archetype count
     */
    public int getArchetypeCount() {
        return archetypes.size();
    }

    private Archetype getOrCreateArchetype(Set<Class<?>> types) {
        final var key = new ArchetypeKey(types);
        return archetypes.computeIfAbsent(key, k -> new Archetype(types));
    }

    /**
     * A key for the archetypes map that pre-computes and caches the hash code.
     * Uses a sorted array for canonical representation and efficient equality checks.
     */
    private static final class ArchetypeKey {
        private final Class<?>[] types;
        private final int hash;

        ArchetypeKey(Set<Class<?>> typeSet) {
            this.types = typeSet.toArray(new Class<?>[typeSet.size()]);
            Arrays.sort(this.types, (a, b) -> a.getName().compareTo(b.getName()));
            this.hash = Arrays.hashCode(this.types);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArchetypeKey that = (ArchetypeKey) o;
            return Arrays.equals(types, that.types);
        }
    }
}
