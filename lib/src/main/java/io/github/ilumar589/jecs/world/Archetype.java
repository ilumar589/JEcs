package io.github.ilumar589.jecs.world;

import io.github.ilumar589.jecs.component.Component;
import io.github.ilumar589.jecs.entity.Entity;

import java.util.*;

/**
 * Represents an archetype - a unique combination of component types.
 * Entities with the same component types are stored together in the same archetype
 * for cache-efficient iteration.
 * 
 * <h2>Storage Model: Struct-of-Arrays (SoA)</h2>
 * Uses columnar storage: each component type has its own {@link ComponentStore},
 * and all stores share the same index for a given entity. This Struct-of-Arrays
 * layout provides:
 * <ul>
 *   <li><b>Cache locality:</b> Components of the same type are stored contiguously,
 *       maximizing cache line utilization when iterating over a single component type</li>
 *   <li><b>Reduced pointer indirection:</b> Array-based storage minimizes reference chasing</li>
 *   <li><b>Efficient iteration:</b> Sequential memory access patterns leverage CPU prefetching</li>
 * </ul>
 * 
 * <h2>Project Valhalla Preparation</h2>
 * This implementation is designed with Project Valhalla's value types in mind:
 * <ul>
 *   <li>{@link ComponentStore} uses {@code Object[]} which can be migrated to 
 *       flattened value type arrays when available</li>
 *   <li>Entity is already a record and a candidate for {@code value class} conversion</li>
 *   <li>Component implementations (records) are ideal value type candidates</li>
 * </ul>
 * 
 * <h2>Cache-Friendly Iteration Pattern</h2>
 * For optimal performance when processing components:
 * <pre>{@code
 * // Get raw arrays for sequential access
 * List<Entity> entities = archetype.getEntities();
 * ComponentStore<Position> positions = archetype.getComponentStore(Position.class);
 * Object[] posData = positions.getRawData();
 * int size = positions.size();
 * 
 * for (int i = 0; i < size; i++) {
 *     Position pos = (Position) posData[i];
 *     Entity entity = entities.get(i);
 *     // Process - sequential access maximizes cache hits
 * }
 * }</pre>
 */
public final class Archetype {
    /**
     * Default initial capacity for component stores.
     * Balances memory usage against reallocation overhead for typical use cases.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    
    private final Set<Class<? extends Component>> componentTypes;
    
    /**
     * Stores entity references in insertion order.
     * Entity IDs are stored near their component data indices for cache efficiency.
     * 
     * <h3>Valhalla Note</h3>
     * When Entity becomes a value type, this could be stored as a primitive int[]
     * for entity IDs, eliminating object headers and reference indirection.
     */
    private final List<Entity> entities;
    
    /**
     * Type-safe component storage using array-backed stores.
     * Each store maintains contiguous memory for its component type.
     */
    private final Map<Class<? extends Component>, ComponentStore<? extends Component>> componentColumns;
    
    private final Map<Entity, Integer> entityToIndex;

    /**
     * Creates a new archetype for the given set of component types.
     *
     * @param componentTypes the set of component types that define this archetype
     */
    public Archetype(Set<Class<? extends Component>> componentTypes) {
        this(componentTypes, DEFAULT_INITIAL_CAPACITY);
    }
    
    /**
     * Creates a new archetype with a capacity hint for better memory pre-allocation.
     * Use this constructor when the expected number of entities is known.
     *
     * @param componentTypes the set of component types that define this archetype
     * @param initialCapacity hint for initial storage capacity
     */
    @SuppressWarnings("unchecked")
    public Archetype(Set<Class<? extends Component>> componentTypes, int initialCapacity) {
        this.componentTypes = Set.copyOf(componentTypes);
        this.entities = new ArrayList<>(initialCapacity);
        this.componentColumns = new HashMap<>();
        this.entityToIndex = new HashMap<>();

        for (Class<? extends Component> type : componentTypes) {
            componentColumns.put(type, new ComponentStore<>((Class<Component>) type, initialCapacity));
        }
    }

    /**
     * Returns the set of component types that define this archetype.
     *
     * @return an unmodifiable set of component types
     */
    public Set<Class<? extends Component>> getComponentTypes() {
        return componentTypes;
    }

    /**
     * Returns all entities in this archetype.
     *
     * @return an unmodifiable list of entities
     */
    public List<Entity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    /**
     * Returns the number of entities in this archetype.
     *
     * @return the entity count
     */
    public int size() {
        return entities.size();
    }

    /**
     * Checks if this archetype contains the specified entity.
     *
     * @param entity the entity to check
     * @return true if the entity exists in this archetype
     */
    public boolean hasEntity(Entity entity) {
        return entityToIndex.containsKey(entity);
    }

    /**
     * Adds an entity with its components to this archetype.
     *
     * @param entity the entity to add
     * @param components the components for the entity (must match archetype's component types)
     * @throws IllegalArgumentException if components don't match archetype's types
     */
    @SuppressWarnings("unchecked")
    public void addEntity(Entity entity, Map<Class<? extends Component>, Component> components) {
        if (entityToIndex.containsKey(entity)) {
            throw new IllegalArgumentException("Entity already exists in archetype: " + entity);
        }

        if (!components.keySet().equals(componentTypes)) {
            throw new IllegalArgumentException("Component types don't match archetype. Expected: " 
                    + componentTypes + ", got: " + components.keySet());
        }

        int index = entities.size();
        entities.add(entity);
        entityToIndex.put(entity, index);

        for (var entry : components.entrySet()) {
            ComponentStore<Component> store = (ComponentStore<Component>) componentColumns.get(entry.getKey());
            store.add(entry.getValue());
        }
    }

    /**
     * Removes an entity from this archetype.
     * Uses swap-and-pop to maintain contiguous storage.
     *
     * @param entity the entity to remove
     * @return the components that were associated with the entity
     * @throws IllegalArgumentException if entity doesn't exist in this archetype
     */
    @SuppressWarnings("unchecked")
    public Map<Class<? extends Component>, Component> removeEntity(Entity entity) {
        Integer index = entityToIndex.get(entity);
        if (index == null) {
            throw new IllegalArgumentException("Entity doesn't exist in archetype: " + entity);
        }

        Map<Class<? extends Component>, Component> removedComponents = new HashMap<>();
        int lastIndex = entities.size() - 1;

        for (var entry : componentColumns.entrySet()) {
            ComponentStore<Component> store = (ComponentStore<Component>) entry.getValue();
            Component removedComponent = store.get(index);
            removedComponents.put(entry.getKey(), removedComponent);

            // Use swap-and-pop for efficient removal
            if (index != lastIndex) {
                store.set(index, store.get(lastIndex));
            }
            store.removeSwapPop(lastIndex);
        }

        if (index != lastIndex) {
            Entity lastEntity = entities.get(lastIndex);
            entities.set(index, lastEntity);
            entityToIndex.put(lastEntity, index);
        }
        entities.removeLast();
        entityToIndex.remove(entity);

        return removedComponents;
    }

    /**
     * Gets a component for an entity.
     *
     * @param entity the entity
     * @param type the component type
     * @param <T> the component type
     * @return the component, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Entity entity, Class<T> type) {
        Integer index = entityToIndex.get(entity);
        if (index == null) {
            return null;
        }

        ComponentStore<? extends Component> store = componentColumns.get(type);
        if (store == null) {
            return null;
        }

        return (T) store.get(index);
    }

    /**
     * Sets a component for an entity.
     *
     * @param entity the entity
     * @param component the component to set
     * @throws IllegalArgumentException if entity doesn't exist or component type not in archetype
     */
    @SuppressWarnings("unchecked")
    public void setComponent(Entity entity, Component component) {
        Integer index = entityToIndex.get(entity);
        if (index == null) {
            throw new IllegalArgumentException("Entity doesn't exist in archetype: " + entity);
        }

        Class<? extends Component> type = component.getClass();
        ComponentStore<Component> store = (ComponentStore<Component>) componentColumns.get(type);
        if (store == null) {
            throw new IllegalArgumentException("Component type not in archetype: " + type);
        }

        store.set(index, component);
    }

    /**
     * Gets the component column for a specific type.
     *
     * @param type the component type
     * @param <T> the component type
     * @return an unmodifiable list of components of the specified type
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> List<T> getComponentColumn(Class<T> type) {
        ComponentStore<? extends Component> store = componentColumns.get(type);
        if (store == null) {
            return Collections.emptyList();
        }
        return (List<T>) store.asList();
    }
    
    /**
     * Gets the raw component store for a specific type.
     * For performance-critical iteration, use this to access the underlying array.
     *
     * @param type the component type
     * @param <T> the component type
     * @return the component store, or null if the type is not in this archetype
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> ComponentStore<T> getComponentStore(Class<T> type) {
        return (ComponentStore<T>) componentColumns.get(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Archetype archetype = (Archetype) o;
        return Objects.equals(componentTypes, archetype.componentTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentTypes);
    }

    @Override
    public String toString() {
        return "Archetype{" +
                "componentTypes=" + componentTypes +
                ", entityCount=" + entities.size() +
                '}';
    }
}
