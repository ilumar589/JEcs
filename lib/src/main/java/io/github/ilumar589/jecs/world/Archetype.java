package io.github.ilumar589.jecs.world;

import io.github.ilumar589.jecs.component.Component;
import io.github.ilumar589.jecs.entity.Entity;

import java.util.*;

/**
 * Represents an archetype - a unique combination of component types.
 * Entities with the same component types are stored together in the same archetype
 * for cache-efficient iteration.
 * <p>
 * Uses columnar storage: each component type has its own list, and all lists
 * share the same index for a given entity.
 */
public final class Archetype {
    private final Set<Class<? extends Component>> componentTypes;
    private final List<Entity> entities;
    private final Map<Class<? extends Component>, List<Component>> componentColumns;
    private final Map<Entity, Integer> entityToIndex;

    /**
     * Creates a new archetype for the given set of component types.
     *
     * @param componentTypes the set of component types that define this archetype
     */
    public Archetype(Set<Class<? extends Component>> componentTypes) {
        this.componentTypes = Set.copyOf(componentTypes);
        this.entities = new ArrayList<>();
        this.componentColumns = new HashMap<>();
        this.entityToIndex = new HashMap<>();

        for (Class<? extends Component> type : componentTypes) {
            componentColumns.put(type, new ArrayList<>());
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
            componentColumns.get(entry.getKey()).add(entry.getValue());
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
    public Map<Class<? extends Component>, Component> removeEntity(Entity entity) {
        Integer index = entityToIndex.get(entity);
        if (index == null) {
            throw new IllegalArgumentException("Entity doesn't exist in archetype: " + entity);
        }

        Map<Class<? extends Component>, Component> removedComponents = new HashMap<>();
        int lastIndex = entities.size() - 1;

        for (var entry : componentColumns.entrySet()) {
            List<Component> column = entry.getValue();
            Component removedComponent = column.get(index);
            removedComponents.put(entry.getKey(), removedComponent);

            if (index != lastIndex) {
                column.set(index, column.get(lastIndex));
            }
            column.remove(lastIndex);
        }

        if (index != lastIndex) {
            Entity lastEntity = entities.get(lastIndex);
            entities.set(index, lastEntity);
            entityToIndex.put(lastEntity, index);
        }
        entities.remove(lastIndex);
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

        List<Component> column = componentColumns.get(type);
        if (column == null) {
            return null;
        }

        return (T) column.get(index);
    }

    /**
     * Sets a component for an entity.
     *
     * @param entity the entity
     * @param component the component to set
     * @throws IllegalArgumentException if entity doesn't exist or component type not in archetype
     */
    public void setComponent(Entity entity, Component component) {
        Integer index = entityToIndex.get(entity);
        if (index == null) {
            throw new IllegalArgumentException("Entity doesn't exist in archetype: " + entity);
        }

        Class<? extends Component> type = component.getClass();
        List<Component> column = componentColumns.get(type);
        if (column == null) {
            throw new IllegalArgumentException("Component type not in archetype: " + type);
        }

        column.set(index, component);
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
        List<Component> column = componentColumns.get(type);
        if (column == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList((List<T>) column);
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
