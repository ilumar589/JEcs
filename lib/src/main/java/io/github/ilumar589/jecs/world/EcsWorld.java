package io.github.ilumar589.jecs.world;

import io.github.ilumar589.jecs.component.Component;
import io.github.ilumar589.jecs.entity.Entity;

import java.util.*;
import java.util.function.Consumer;

/**
 * The main entry point for the ECS framework.
 * Manages entities, components, and archetypes.
 */
public final class EcsWorld {
    private int nextEntityId = 0;
    private final Map<Entity, Archetype> entityToArchetype;
    private final Map<Set<Class<? extends Component>>, Archetype> archetypes;

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
     * @param components the initial components for the entity
     * @return the newly created entity
     */
    public Entity createEntity(Component... components) {
        Entity entity = new Entity(nextEntityId++);

        if (components.length == 0) {
            return entity;
        }

        Set<Class<? extends Component>> typeSet = new HashSet<>();
        Map<Class<? extends Component>, Component> componentMap = new HashMap<>();

        for (Component component : components) {
            Class<? extends Component> type = component.getClass();
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
    public <T extends Component> T getComponent(Entity entity, Class<T> type) {
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
    public boolean hasComponent(Entity entity, Class<? extends Component> type) {
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
     * @param entity the entity
     * @param component the component to add
     * @throws IllegalArgumentException if the entity doesn't exist or already has this component type
     */
    public void addComponent(Entity entity, Component component) {
        Archetype oldArchetype = entityToArchetype.get(entity);

        Set<Class<? extends Component>> newTypes = new HashSet<>();
        Map<Class<? extends Component>, Component> componentMap = new HashMap<>();

        Class<? extends Component> newType = component.getClass();

        if (oldArchetype != null) {
            if (oldArchetype.getComponentTypes().contains(newType)) {
                throw new IllegalArgumentException("Entity already has component of type: " + newType);
            }

            Map<Class<? extends Component>, Component> oldComponents = oldArchetype.removeEntity(entity);
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
    public void removeComponent(Entity entity, Class<? extends Component> type) {
        Archetype oldArchetype = entityToArchetype.get(entity);

        if (oldArchetype == null) {
            throw new IllegalArgumentException("Entity doesn't exist: " + entity);
        }

        if (!oldArchetype.getComponentTypes().contains(type)) {
            throw new IllegalArgumentException("Entity doesn't have component of type: " + type);
        }

        Map<Class<? extends Component>, Component> oldComponents = oldArchetype.removeEntity(entity);
        oldComponents.remove(type);

        if (oldComponents.isEmpty()) {
            entityToArchetype.remove(entity);
        } else {
            Set<Class<? extends Component>> newTypes = new HashSet<>(oldComponents.keySet());
            Archetype newArchetype = getOrCreateArchetype(newTypes);
            newArchetype.addEntity(entity, oldComponents);
            entityToArchetype.put(entity, newArchetype);
        }
    }

    /**
     * Sets (updates) a component on an entity.
     * The entity must already have a component of this type.
     *
     * @param entity the entity
     * @param component the new component value
     * @throws IllegalArgumentException if the entity doesn't have this component type
     */
    public void setComponent(Entity entity, Component component) {
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
    public final List<Entity> query(Class<? extends Component>... types) {
        Set<Class<? extends Component>> requiredTypes = Set.of(types);
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
    public final void forEach(Consumer<Entity> consumer, Class<? extends Component>... types) {
        Set<Class<? extends Component>> requiredTypes = Set.of(types);

        for (Archetype archetype : archetypes.values()) {
            if (archetype.getComponentTypes().containsAll(requiredTypes)) {
                for (Entity entity : archetype.getEntities()) {
                    consumer.accept(entity);
                }
            }
        }
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

    private Archetype getOrCreateArchetype(Set<Class<? extends Component>> types) {
        return archetypes.computeIfAbsent(types, Archetype::new);
    }
}
