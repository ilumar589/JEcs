package io.github.ilumar589.jecs.query;

import io.github.ilumar589.jecs.entity.Entity;
import io.github.ilumar589.jecs.world.Archetype;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A fluent builder for component-focused queries.
 * Allows querying entities by included and excluded component types,
 * without exposing implementation details like Entity or Archetype.
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Query with one component
 * world.componentQuery()
 *     .with(Position.class)
 *     .forEach(Position.class, pos -> {
 *         System.out.println("Pos: " + pos);
 *     });
 *
 * // Query with two components
 * world.componentQuery()
 *     .with(Position.class, Velocity.class)
 *     .forEach(Position.class, Velocity.class, (pos, vel) -> {
 *         System.out.println("Pos: " + pos + ", Vel: " + vel);
 *     });
 *
 * // Query with exclusion
 * world.componentQuery()
 *     .with(Position.class, Health.class)
 *     .without(Dead.class)
 *     .forEach(Position.class, Health.class, (pos, health) -> {
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
 * <h2>Thread Safety</h2>
 * ComponentQuery instances are not thread-safe. Each thread should create
 * its own query instance via {@code world.componentQuery()}.
 */
public final class ComponentQuery {

    private final Map<ArchetypeKey, Archetype> archetypes;
    private final Map<Entity, Archetype> entityToArchetype;
    private final QueryExecutor queryExecutor;

    private final Set<Class<?>> includedTypes = new LinkedHashSet<>();
    private final Set<Class<?>> excludedTypes = new HashSet<>();

    /**
     * Creates a new ComponentQuery.
     * This constructor is typically called internally by EcsWorld.
     *
     * @param archetypes the map of archetype keys to archetypes
     * @param entityToArchetype the map of entities to their archetypes
     * @param queryExecutor the executor for setting components
     */
    public ComponentQuery(Map<ArchetypeKey, Archetype> archetypes,
                          Map<Entity, Archetype> entityToArchetype,
                          QueryExecutor queryExecutor) {
        this.archetypes = archetypes;
        this.entityToArchetype = entityToArchetype;
        this.queryExecutor = queryExecutor;
    }

    // ==================== Builder Methods ====================

    /**
     * Specifies the component types that entities must have.
     *
     * @param types the required component types
     * @return this query for chaining
     */
    @SafeVarargs
    public final ComponentQuery with(Class<?>... types) {
        for (Class<?> type : types) {
            includedTypes.add(type);
        }
        return this;
    }

    /**
     * Specifies component types that entities must NOT have.
     *
     * @param types the excluded component types
     * @return this query for chaining
     */
    @SafeVarargs
    public final ComponentQuery without(Class<?>... types) {
        for (Class<?> type : types) {
            excludedTypes.add(type);
        }
        return this;
    }

    // ==================== Terminal Operations ====================

    /**
     * Iterates over all matching entities, providing a single component.
     *
     * @param typeA the class of the component type
     * @param consumer the consumer for the component
     * @param <A> the component type
     */
    public <A> void forEach(Class<A> typeA, Consumer<A> consumer) {
        includedTypes.add(typeA);

        for (Archetype archetype : getMatchingArchetypes()) {
            for (Entity entity : archetype.getEntities()) {
                A a = archetype.getComponent(entity, typeA);
                consumer.accept(a);
            }
        }
    }

    /**
     * Iterates over all matching entities, providing two components.
     *
     * @param typeA the class of the first component type
     * @param typeB the class of the second component type
     * @param consumer the consumer for the components
     * @param <A> the first component type
     * @param <B> the second component type
     */
    public <A, B> void forEach(Class<A> typeA, Class<B> typeB, BiConsumer<A, B> consumer) {
        includedTypes.add(typeA);
        includedTypes.add(typeB);

        for (Archetype archetype : getMatchingArchetypes()) {
            for (Entity entity : archetype.getEntities()) {
                A a = archetype.getComponent(entity, typeA);
                B b = archetype.getComponent(entity, typeB);
                consumer.accept(a, b);
            }
        }
    }

    /**
     * Iterates over all matching entities, providing three components.
     *
     * @param typeA the class of the first component type
     * @param typeB the class of the second component type
     * @param typeC the class of the third component type
     * @param consumer the consumer for the components
     * @param <A> the first component type
     * @param <B> the second component type
     * @param <C> the third component type
     */
    public <A, B, C> void forEach(Class<A> typeA, Class<B> typeB, Class<C> typeC, 
                                   TriConsumer<A, B, C> consumer) {
        includedTypes.add(typeA);
        includedTypes.add(typeB);
        includedTypes.add(typeC);

        for (Archetype archetype : getMatchingArchetypes()) {
            for (Entity entity : archetype.getEntities()) {
                A a = archetype.getComponent(entity, typeA);
                B b = archetype.getComponent(entity, typeB);
                C c = archetype.getComponent(entity, typeC);
                consumer.accept(a, b, c);
            }
        }
    }

    /**
     * Iterates over all matching entities, providing four components.
     *
     * @param typeA the class of the first component type
     * @param typeB the class of the second component type
     * @param typeC the class of the third component type
     * @param typeD the class of the fourth component type
     * @param consumer the consumer for the components
     * @param <A> the first component type
     * @param <B> the second component type
     * @param <C> the third component type
     * @param <D> the fourth component type
     */
    public <A, B, C, D> void forEach(Class<A> typeA, Class<B> typeB, Class<C> typeC, 
                                      Class<D> typeD, QuadConsumer<A, B, C, D> consumer) {
        includedTypes.add(typeA);
        includedTypes.add(typeB);
        includedTypes.add(typeC);
        includedTypes.add(typeD);

        for (Archetype archetype : getMatchingArchetypes()) {
            for (Entity entity : archetype.getEntities()) {
                A a = archetype.getComponent(entity, typeA);
                B b = archetype.getComponent(entity, typeB);
                C c = archetype.getComponent(entity, typeC);
                D d = archetype.getComponent(entity, typeD);
                consumer.accept(a, b, c, d);
            }
        }
    }

    /**
     * Returns the results as a list of two-component tuples.
     *
     * @param typeA the class of the first component type
     * @param typeB the class of the second component type
     * @param <A> the first component type
     * @param <B> the second component type
     * @return list of component tuples
     */
    public <A, B> List<ComponentTuple2<A, B>> results2(Class<A> typeA, Class<B> typeB) {
        includedTypes.add(typeA);
        includedTypes.add(typeB);

        List<ComponentTuple2<A, B>> results = new ArrayList<>();
        for (Archetype archetype : getMatchingArchetypes()) {
            for (Entity entity : archetype.getEntities()) {
                A a = archetype.getComponent(entity, typeA);
                B b = archetype.getComponent(entity, typeB);
                results.add(new ComponentTuple2<>(a, b));
            }
        }
        return results;
    }

    /**
     * Returns the results as a list of three-component tuples.
     *
     * @param typeA the class of the first component type
     * @param typeB the class of the second component type
     * @param typeC the class of the third component type
     * @param <A> the first component type
     * @param <B> the second component type
     * @param <C> the third component type
     * @return list of component tuples
     */
    public <A, B, C> List<ComponentTuple3<A, B, C>> results3(Class<A> typeA, Class<B> typeB, 
                                                              Class<C> typeC) {
        includedTypes.add(typeA);
        includedTypes.add(typeB);
        includedTypes.add(typeC);

        List<ComponentTuple3<A, B, C>> results = new ArrayList<>();
        for (Archetype archetype : getMatchingArchetypes()) {
            for (Entity entity : archetype.getEntities()) {
                A a = archetype.getComponent(entity, typeA);
                B b = archetype.getComponent(entity, typeB);
                C c = archetype.getComponent(entity, typeC);
                results.add(new ComponentTuple3<>(a, b, c));
            }
        }
        return results;
    }

    /**
     * Returns the results as a list of four-component tuples.
     *
     * @param typeA the class of the first component type
     * @param typeB the class of the second component type
     * @param typeC the class of the third component type
     * @param typeD the class of the fourth component type
     * @param <A> the first component type
     * @param <B> the second component type
     * @param <C> the third component type
     * @param <D> the fourth component type
     * @return list of component tuples
     */
    public <A, B, C, D> List<ComponentTuple4<A, B, C, D>> results4(Class<A> typeA, Class<B> typeB, 
                                                                    Class<C> typeC, Class<D> typeD) {
        includedTypes.add(typeA);
        includedTypes.add(typeB);
        includedTypes.add(typeC);
        includedTypes.add(typeD);

        List<ComponentTuple4<A, B, C, D>> results = new ArrayList<>();
        for (Archetype archetype : getMatchingArchetypes()) {
            for (Entity entity : archetype.getEntities()) {
                A a = archetype.getComponent(entity, typeA);
                B b = archetype.getComponent(entity, typeB);
                C c = archetype.getComponent(entity, typeC);
                D d = archetype.getComponent(entity, typeD);
                results.add(new ComponentTuple4<>(a, b, c, d));
            }
        }
        return results;
    }

    /**
     * Returns the count of matching entities.
     *
     * @return the number of matching entities
     */
    public int count() {
        int count = 0;
        for (Archetype archetype : getMatchingArchetypes()) {
            count += archetype.size();
        }
        return count;
    }

    /**
     * Checks if any entity matches the query.
     *
     * @return true if at least one entity matches
     */
    public boolean any() {
        for (Archetype archetype : getMatchingArchetypes()) {
            if (archetype.size() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Modifies the first component of matching entities using the provided function.
     *
     * @param typeA the class of the component type to modify
     * @param modifier the function to transform the component
     * @param <A> the component type
     */
    public <A> void modify(Class<A> typeA, Function<A, A> modifier) {
        includedTypes.add(typeA);

        for (Archetype archetype : getMatchingArchetypes()) {
            for (Entity entity : archetype.getEntities()) {
                A a = archetype.getComponent(entity, typeA);
                A newA = modifier.apply(a);
                queryExecutor.setComponent(entity, newA);
            }
        }
    }

    /**
     * Conditionally modifies the first component of matching entities.
     *
     * @param typeA the class of the component type to modify
     * @param predicate the condition to check before modifying
     * @param modifier the function to transform the component
     * @param <A> the component type
     */
    public <A> void modifyIf(Class<A> typeA, Predicate<A> predicate, Function<A, A> modifier) {
        includedTypes.add(typeA);

        for (Archetype archetype : getMatchingArchetypes()) {
            for (Entity entity : archetype.getEntities()) {
                A a = archetype.getComponent(entity, typeA);
                if (predicate.test(a)) {
                    A newA = modifier.apply(a);
                    queryExecutor.setComponent(entity, newA);
                }
            }
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Returns archetypes that match the inclusion and exclusion criteria.
     */
    private List<Archetype> getMatchingArchetypes() {
        List<Archetype> matching = new ArrayList<>();

        for (Archetype archetype : archetypes.values()) {
            Set<Class<?>> archetypeTypes = archetype.getComponentTypes();

            // Check inclusion: archetype must have all included types
            if (!archetypeTypes.containsAll(includedTypes)) {
                continue;
            }

            // Check exclusion: archetype must not have any excluded types
            boolean hasExcluded = false;
            for (Class<?> excluded : excludedTypes) {
                if (archetypeTypes.contains(excluded)) {
                    hasExcluded = true;
                    break;
                }
            }
            if (hasExcluded) {
                continue;
            }

            matching.add(archetype);
        }

        return matching;
    }

    /**
     * Functional interface for setting components on entities.
     * Used internally to delegate component modification back to EcsWorld.
     */
    @FunctionalInterface
    public interface QueryExecutor {
        /**
         * Sets a component on the specified entity.
         *
         * @param entity the entity
         * @param component the new component value
         */
        void setComponent(Entity entity, Object component);
    }

    /**
     * A key for the archetypes map.
     * This is a simplified version exposed for query construction.
     */
    public static final class ArchetypeKey {
        private final Class<?>[] types;
        private final int hash;

        public ArchetypeKey(Set<Class<?>> typeSet) {
            this.types = typeSet.toArray(new Class<?>[0]);
            java.util.Arrays.sort(this.types, (a, b) -> a.getName().compareTo(b.getName()));
            this.hash = java.util.Arrays.hashCode(this.types);
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
            return java.util.Arrays.equals(types, that.types);
        }
    }
}
