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

    private final Collection<Archetype> archetypes;
    private final QueryExecutor queryExecutor;

    private final Set<Class<?>> includedTypes = new LinkedHashSet<>();
    private final Set<Class<?>> readOnlyTypes = new LinkedHashSet<>();
    private final Set<Class<?>> mutableTypes = new LinkedHashSet<>();
    private final Set<Class<?>> excludedTypes = new HashSet<>();

    /**
     * Creates a new ComponentQuery.
     * This constructor is typically called internally by EcsWorld.
     *
     * @param archetypes the collection of archetypes to query
     * @param queryExecutor the executor for setting components
     */
    public ComponentQuery(Collection<Archetype> archetypes,
                          QueryExecutor queryExecutor) {
        this.archetypes = archetypes;
        this.queryExecutor = queryExecutor;
    }

    // ==================== Builder Methods ====================

    /**
     * Specifies the component types that entities must have.
     * Access control is unspecified - components can be read or modified.
     * For explicit access control, use {@link #withReadOnly} or {@link #withMutable}.
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
     * Specifies component types that entities must have, with read-only access.
     * These components will not be modified during query iteration.
     * 
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.componentQuery()
     *     .withReadOnly(Position.class)
     *     .withMutable(Velocity.class)
     *     .forEach(Position.class, Velocity.class, (pos, vel) -> {
     *         // pos is read-only, vel can be modified
     *     });
     * }</pre>
     *
     * @param types the required read-only component types
     * @return this query for chaining
     */
    @SafeVarargs
    public final ComponentQuery withReadOnly(Class<?>... types) {
        for (Class<?> type : types) {
            includedTypes.add(type);
            readOnlyTypes.add(type);
        }
        return this;
    }

    /**
     * Specifies component types that entities must have, with mutable access.
     * These components may be modified during query iteration.
     * 
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.componentQuery()
     *     .withReadOnly(Velocity.class)
     *     .withMutable(Position.class)
     *     .forEach(Position.class, Velocity.class, (pos, vel) -> {
     *         // Position can be modified based on Velocity
     *     });
     * }</pre>
     *
     * @param types the required mutable component types
     * @return this query for chaining
     */
    @SafeVarargs
    public final ComponentQuery withMutable(Class<?>... types) {
        for (Class<?> type : types) {
            includedTypes.add(type);
            mutableTypes.add(type);
        }
        return this;
    }

    /**
     * Checks if a component type is marked as read-only.
     *
     * @param type the component type
     * @return true if the component is read-only
     */
    public boolean isReadOnly(Class<?> type) {
        return readOnlyTypes.contains(type);
    }

    /**
     * Checks if a component type is marked as mutable.
     *
     * @param type the component type
     * @return true if the component is mutable
     */
    public boolean isMutable(Class<?> type) {
        return mutableTypes.contains(type);
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
        for (Archetype archetype : getMatchingArchetypes(Set.of(typeA))) {
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
        for (Archetype archetype : getMatchingArchetypes(Set.of(typeA, typeB))) {
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
        for (Archetype archetype : getMatchingArchetypes(Set.of(typeA, typeB, typeC))) {
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
        for (Archetype archetype : getMatchingArchetypes(Set.of(typeA, typeB, typeC, typeD))) {
            for (Entity entity : archetype.getEntities()) {
                A a = archetype.getComponent(entity, typeA);
                B b = archetype.getComponent(entity, typeB);
                C c = archetype.getComponent(entity, typeC);
                D d = archetype.getComponent(entity, typeD);
                consumer.accept(a, b, c, d);
            }
        }
    }

    // ==================== Access-Controlled Terminal Operations ====================

    /**
     * Iterates over matching entities with one mutable component.
     * Changes made via {@link Mutable#set} or {@link Mutable#update} are automatically saved.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.componentQuery()
     *     .withMutable(Position.class)
     *     .forEachMutable(Position.class, pos -> {
     *         pos.update(p -> new Position(p.x() + 1, p.y(), p.z()));
     *     });
     * }</pre>
     *
     * @param typeA the class of the mutable component type
     * @param consumer the consumer that receives a Mutable wrapper
     * @param <A> the component type
     */
    public <A> void forEachMutable(Class<A> typeA, Consumer<Mutable<A>> consumer) {
        for (Archetype archetype : getMatchingArchetypes(Set.of(typeA))) {
            for (Entity entity : archetype.getEntities()) {
                A a = archetype.getComponent(entity, typeA);
                Mutable<A> mutableA = new Mutable<>(a);
                consumer.accept(mutableA);
                if (mutableA.isModified()) {
                    queryExecutor.setComponent(entity, mutableA.get());
                }
            }
        }
    }

    /**
     * Iterates over matching entities with one read-only and one mutable component.
     * Changes made via the Mutable wrapper are automatically saved.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.componentQuery()
     *     .withReadOnly(Velocity.class)
     *     .withMutable(Position.class)
     *     .forEachWithAccess(Position.class, Velocity.class, (pos, vel) -> {
     *         pos.update(p -> new Position(
     *             p.x() + vel.get().dx(),
     *             p.y() + vel.get().dy(),
     *             p.z() + vel.get().dz()
     *         ));
     *     });
     * }</pre>
     *
     * @param mutableType the class of the mutable component type
     * @param readOnlyType the class of the read-only component type
     * @param consumer the consumer that receives Mutable and ReadOnly wrappers
     * @param <A> the mutable component type
     * @param <B> the read-only component type
     */
    public <A, B> void forEachWithAccess(Class<A> mutableType, Class<B> readOnlyType,
                                          BiConsumer<Mutable<A>, ReadOnly<B>> consumer) {
        for (Archetype archetype : getMatchingArchetypes(Set.of(mutableType, readOnlyType))) {
            for (Entity entity : archetype.getEntities()) {
                A a = archetype.getComponent(entity, mutableType);
                B b = archetype.getComponent(entity, readOnlyType);
                Mutable<A> mutableA = new Mutable<>(a);
                ReadOnly<B> readOnlyB = new ReadOnly<>(b);
                consumer.accept(mutableA, readOnlyB);
                if (mutableA.isModified()) {
                    queryExecutor.setComponent(entity, mutableA.get());
                }
            }
        }
    }

    /**
     * Iterates over matching entities with two read-only and one mutable component.
     *
     * @param mutableType the class of the mutable component type
     * @param readOnly1 the class of the first read-only component type
     * @param readOnly2 the class of the second read-only component type
     * @param consumer the consumer that receives wrappers
     * @param <A> the mutable component type
     * @param <B> the first read-only component type
     * @param <C> the second read-only component type
     */
    public <A, B, C> void forEachWithAccess(Class<A> mutableType, Class<B> readOnly1, Class<C> readOnly2,
                                             TriConsumer<Mutable<A>, ReadOnly<B>, ReadOnly<C>> consumer) {
        for (Archetype archetype : getMatchingArchetypes(Set.of(mutableType, readOnly1, readOnly2))) {
            for (Entity entity : archetype.getEntities()) {
                A a = archetype.getComponent(entity, mutableType);
                B b = archetype.getComponent(entity, readOnly1);
                C c = archetype.getComponent(entity, readOnly2);
                Mutable<A> mutableA = new Mutable<>(a);
                consumer.accept(mutableA, new ReadOnly<>(b), new ReadOnly<>(c));
                if (mutableA.isModified()) {
                    queryExecutor.setComponent(entity, mutableA.get());
                }
            }
        }
    }

    // ==================== Results Operations ====================

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
        List<ComponentTuple2<A, B>> results = new ArrayList<>();
        for (Archetype archetype : getMatchingArchetypes(Set.of(typeA, typeB))) {
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
        List<ComponentTuple3<A, B, C>> results = new ArrayList<>();
        for (Archetype archetype : getMatchingArchetypes(Set.of(typeA, typeB, typeC))) {
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
        List<ComponentTuple4<A, B, C, D>> results = new ArrayList<>();
        for (Archetype archetype : getMatchingArchetypes(Set.of(typeA, typeB, typeC, typeD))) {
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
        for (Archetype archetype : getMatchingArchetypes(Set.of(typeA))) {
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
        for (Archetype archetype : getMatchingArchetypes(Set.of(typeA))) {
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
        return getMatchingArchetypes(Set.of());
    }

    /**
     * Returns archetypes that match the inclusion and exclusion criteria,
     * plus the additional required types.
     */
    private List<Archetype> getMatchingArchetypes(Set<Class<?>> additionalRequired) {
        List<Archetype> matching = new ArrayList<>();

        for (Archetype archetype : archetypes) {
            Set<Class<?>> archetypeTypes = archetype.getComponentTypes();

            // Check inclusion: archetype must have all included types
            if (!archetypeTypes.containsAll(includedTypes)) {
                continue;
            }

            // Check inclusion: archetype must have all additional required types
            if (!archetypeTypes.containsAll(additionalRequired)) {
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
}
