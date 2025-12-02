package io.github.ilumar589.jecs.query;

import io.github.ilumar589.jecs.world.Archetype;
import io.github.ilumar589.jecs.world.ComponentReader;
import io.github.ilumar589.jecs.world.ComponentWriter;

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
 * // Query with one component using generic forEach
 * world.componentQuery()
 *     .withMutable(Position.class)
 *     .forEach(wrappers -> {
 *         Mutable<Position> pos = (Mutable<Position>) wrappers[0];
 *         pos.update(p -> new Position(p.x() + 10, p.y(), p.z()));
 *     }, Position.class);
 *
 * // Query with multiple mutable and read-only components
 * world.componentQuery()
 *     .withMutable(Position.class, Health.class)
 *     .withReadOnly(Velocity.class, Gravity.class)
 *     .forEach(wrappers -> {
 *         Mutable<Position> pos = (Mutable<Position>) wrappers[0];
 *         Mutable<Health> health = (Mutable<Health>) wrappers[1];
 *         ReadOnly<Velocity> vel = (ReadOnly<Velocity>) wrappers[2];
 *         ReadOnly<Gravity> grav = (ReadOnly<Gravity>) wrappers[3];
 *         
 *         pos.update(p -> applyPhysics(p, vel.get(), grav.get()));
 *         // vel.set(...) would throw UnsupportedOperationException
 *     }, Position.class, Health.class, Velocity.class, Gravity.class);
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
 * 
 * <h2>Valhalla Readiness</h2>
 * This API is designed to be compatible with Project Valhalla.
 * Component wrappers can become value classes with zero code changes.
 */
public final class ComponentQuery {

    private final Collection<Archetype> archetypes;

    private final Set<Class<?>> includedTypes = new LinkedHashSet<>();
    private final Set<Class<?>> readOnlyTypes = new LinkedHashSet<>();
    private final Set<Class<?>> mutableTypes = new LinkedHashSet<>();
    private final Set<Class<?>> excludedTypes = new HashSet<>();

    /**
     * Creates a new ComponentQuery.
     * This constructor is typically called internally by EcsWorld.
     *
     * @param archetypes the collection of archetypes to query
     */
    public ComponentQuery(Collection<Archetype> archetypes) {
        this.archetypes = archetypes;
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
     * Attempting to modify a read-only component via {@link ReadOnly#set} or 
     * {@link ReadOnly#update} will throw {@link UnsupportedOperationException}.
     * 
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.componentQuery()
     *     .withReadOnly(Velocity.class)
     *     .withMutable(Position.class)
     *     .forEach(wrappers -> {
     *         Mutable<Position> pos = (Mutable<Position>) wrappers[0];
     *         ReadOnly<Velocity> vel = (ReadOnly<Velocity>) wrappers[1];
     *         // pos can be modified, vel cannot
     *     }, Position.class, Velocity.class);
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
     * Writes via {@link Mutable#set} or {@link Mutable#update} are applied
     * immediately to the primitive arrays.
     * 
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.componentQuery()
     *     .withMutable(Position.class)
     *     .forEach(wrappers -> {
     *         Mutable<Position> pos = (Mutable<Position>) wrappers[0];
     *         pos.update(p -> new Position(p.x() + 10, p.y(), p.z()));
     *     }, Position.class);
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
     * Generic forEach that handles any number of components with automatic wrapper selection.
     * Components are wrapped as {@link Mutable} or {@link ReadOnly} based on 
     * {@link #withMutable} / {@link #withReadOnly} declarations.
     * 
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.componentQuery()
     *     .withMutable(Position.class, Health.class)
     *     .withReadOnly(Velocity.class, Gravity.class, Weight.class)
     *     .forEach(wrappers -> {
     *         Mutable<Position> pos = (Mutable<Position>) wrappers[0];
     *         Mutable<Health> health = (Mutable<Health>) wrappers[1];
     *         ReadOnly<Velocity> vel = (ReadOnly<Velocity>) wrappers[2];
     *         ReadOnly<Gravity> grav = (ReadOnly<Gravity>) wrappers[3];
     *         ReadOnly<Weight> weight = (ReadOnly<Weight>) wrappers[4];
     *         
     *         // Use components...
     *         pos.update(p -> applyPhysics(p, vel.get(), grav.get()));
     *         
     *         // Trying this throws exception:
     *         // vel.set(...);  // UnsupportedOperationException!
     *     }, Position.class, Health.class, Velocity.class, Gravity.class, Weight.class);
     * }</pre>
     * 
     * <h2>Direct Primitive Access</h2>
     * {@link Mutable} wrappers write directly to primitive arrays - no staging or 
     * write-back is needed. Changes are visible immediately.
     * 
     * <h2>Valhalla Readiness</h2>
     * When Project Valhalla lands, wrappers can become value classes for better performance.
     *
     * @param consumer the consumer that receives an array of component wrappers
     * @param types the component types in the order they should appear in the wrapper array
     */
    @SuppressWarnings("unchecked")
    public void forEach(Consumer<Object[]> consumer, Class<?>... types) {
        Set<Class<?>> typeSet = new LinkedHashSet<>(Arrays.asList(types));
        
        for (Archetype archetype : getMatchingArchetypes(typeSet)) {
            // Get readers/writers for each component type
            Object[] accessors = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                Class<?> type = types[i];
                if (mutableTypes.contains(type)) {
                    accessors[i] = archetype.getWriter(type);
                } else {
                    accessors[i] = archetype.getReader(type);
                }
            }
            
            // Iterate entities
            int size = archetype.size();
            for (int entityIdx = 0; entityIdx < size; entityIdx++) {
                // Create wrappers for this entity
                Object[] wrappers = new Object[types.length];
                for (int i = 0; i < types.length; i++) {
                    if (mutableTypes.contains(types[i])) {
                        ComponentWriter<?> writer = (ComponentWriter<?>) accessors[i];
                        wrappers[i] = new Mutable(writer, entityIdx);
                    } else {
                        ComponentReader<?> reader = (ComponentReader<?>) accessors[i];
                        wrappers[i] = new ReadOnly(reader, entityIdx);
                    }
                }
                
                // Execute consumer - no write-back needed, Mutable writes directly
                consumer.accept(wrappers);
            }
        }
    }

    // ==================== Type-Safe Helper Methods ====================
    
    /**
     * Iterates over all matching entities, providing a single component.
     * This is a type-safe helper that unwraps the component value.
     *
     * @param typeA the class of the component type
     * @param consumer the consumer for the component
     * @param <A> the component type
     */
    public <A> void forEach(Class<A> typeA, Consumer<A> consumer) {
        Set<Class<?>> typeSet = Set.of(typeA);
        
        for (Archetype archetype : getMatchingArchetypes(typeSet)) {
            ComponentReader<A> reader = archetype.getReader(typeA);
            int size = archetype.size();
            
            for (int i = 0; i < size; i++) {
                A a = reader.read(i);
                consumer.accept(a);
            }
        }
    }

    /**
     * Iterates over all matching entities, providing two components.
     * This is a type-safe helper that unwraps the component values.
     *
     * @param typeA the class of the first component type
     * @param typeB the class of the second component type
     * @param consumer the consumer for the components
     * @param <A> the first component type
     * @param <B> the second component type
     */
    public <A, B> void forEach(Class<A> typeA, Class<B> typeB, BiConsumer<A, B> consumer) {
        Set<Class<?>> typeSet = Set.of(typeA, typeB);
        
        for (Archetype archetype : getMatchingArchetypes(typeSet)) {
            ComponentReader<A> readerA = archetype.getReader(typeA);
            ComponentReader<B> readerB = archetype.getReader(typeB);
            int size = archetype.size();
            
            for (int i = 0; i < size; i++) {
                A a = readerA.read(i);
                B b = readerB.read(i);
                consumer.accept(a, b);
            }
        }
    }

    /**
     * Iterates over all matching entities, providing three components.
     * This is a type-safe helper that unwraps the component values.
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
        Set<Class<?>> typeSet = Set.of(typeA, typeB, typeC);
        
        for (Archetype archetype : getMatchingArchetypes(typeSet)) {
            ComponentReader<A> readerA = archetype.getReader(typeA);
            ComponentReader<B> readerB = archetype.getReader(typeB);
            ComponentReader<C> readerC = archetype.getReader(typeC);
            int size = archetype.size();
            
            for (int i = 0; i < size; i++) {
                A a = readerA.read(i);
                B b = readerB.read(i);
                C c = readerC.read(i);
                consumer.accept(a, b, c);
            }
        }
    }

    /**
     * Iterates over all matching entities, providing four components.
     * This is a type-safe helper that unwraps the component values.
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
        Set<Class<?>> typeSet = Set.of(typeA, typeB, typeC, typeD);
        
        for (Archetype archetype : getMatchingArchetypes(typeSet)) {
            ComponentReader<A> readerA = archetype.getReader(typeA);
            ComponentReader<B> readerB = archetype.getReader(typeB);
            ComponentReader<C> readerC = archetype.getReader(typeC);
            ComponentReader<D> readerD = archetype.getReader(typeD);
            int size = archetype.size();
            
            for (int i = 0; i < size; i++) {
                A a = readerA.read(i);
                B b = readerB.read(i);
                C c = readerC.read(i);
                D d = readerD.read(i);
                consumer.accept(a, b, c, d);
            }
        }
    }

    /**
     * Modifies the first component of matching entities using the provided function.
     * Uses the generic forEach internally with mutable access.
     *
     * @param typeA the class of the component type to modify
     * @param modifier the function to transform the component
     * @param <A> the component type
     */
    @SuppressWarnings("unchecked")
    public <A> void modify(Class<A> typeA, Function<A, A> modifier) {
        // Remember original state to restore after operation
        boolean wasMutable = mutableTypes.contains(typeA);
        boolean wasIncluded = includedTypes.contains(typeA);
        
        // Temporarily mark as mutable for the operation
        mutableTypes.add(typeA);
        includedTypes.add(typeA);
        
        try {
            forEach(wrappers -> {
                Mutable<A> mutableA = (Mutable<A>) wrappers[0];
                A current = mutableA.get();
                A newValue = modifier.apply(current);
                mutableA.set(newValue);
            }, typeA);
        } finally {
            // Restore original state
            if (!wasMutable) {
                mutableTypes.remove(typeA);
            }
            if (!wasIncluded) {
                includedTypes.remove(typeA);
            }
        }
    }

    /**
     * Conditionally modifies the first component of matching entities.
     * Uses the generic forEach internally with mutable access.
     *
     * @param typeA the class of the component type to modify
     * @param predicate the condition to check before modifying
     * @param modifier the function to transform the component
     * @param <A> the component type
     */
    @SuppressWarnings("unchecked")
    public <A> void modifyIf(Class<A> typeA, Predicate<A> predicate, Function<A, A> modifier) {
        // Remember original state to restore after operation
        boolean wasMutable = mutableTypes.contains(typeA);
        boolean wasIncluded = includedTypes.contains(typeA);
        
        // Temporarily mark as mutable for the operation
        mutableTypes.add(typeA);
        includedTypes.add(typeA);
        
        try {
            forEach(wrappers -> {
                Mutable<A> mutableA = (Mutable<A>) wrappers[0];
                A current = mutableA.get();
                if (predicate.test(current)) {
                    A newValue = modifier.apply(current);
                    mutableA.set(newValue);
                }
            }, typeA);
        } finally {
            // Restore original state
            if (!wasMutable) {
                mutableTypes.remove(typeA);
            }
            if (!wasIncluded) {
                includedTypes.remove(typeA);
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
        Set<Class<?>> typeSet = Set.of(typeA, typeB);
        
        for (Archetype archetype : getMatchingArchetypes(typeSet)) {
            ComponentReader<A> readerA = archetype.getReader(typeA);
            ComponentReader<B> readerB = archetype.getReader(typeB);
            int size = archetype.size();
            
            for (int i = 0; i < size; i++) {
                A a = readerA.read(i);
                B b = readerB.read(i);
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
        Set<Class<?>> typeSet = Set.of(typeA, typeB, typeC);
        
        for (Archetype archetype : getMatchingArchetypes(typeSet)) {
            ComponentReader<A> readerA = archetype.getReader(typeA);
            ComponentReader<B> readerB = archetype.getReader(typeB);
            ComponentReader<C> readerC = archetype.getReader(typeC);
            int size = archetype.size();
            
            for (int i = 0; i < size; i++) {
                A a = readerA.read(i);
                B b = readerB.read(i);
                C c = readerC.read(i);
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
        Set<Class<?>> typeSet = Set.of(typeA, typeB, typeC, typeD);
        
        for (Archetype archetype : getMatchingArchetypes(typeSet)) {
            ComponentReader<A> readerA = archetype.getReader(typeA);
            ComponentReader<B> readerB = archetype.getReader(typeB);
            ComponentReader<C> readerC = archetype.getReader(typeC);
            ComponentReader<D> readerD = archetype.getReader(typeD);
            int size = archetype.size();
            
            for (int i = 0; i < size; i++) {
                A a = readerA.read(i);
                B b = readerB.read(i);
                C c = readerC.read(i);
                D d = readerD.read(i);
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
}
