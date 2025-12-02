package io.github.ilumar589.jecs.query;

import io.github.ilumar589.jecs.world.Archetype;
import io.github.ilumar589.jecs.world.ComponentReader;
import io.github.ilumar589.jecs.world.ComponentTypeRegistry;
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
 * <h2>Recommended: Type-Safe forEachWrapper API (1-6 components)</h2>
 * <p>For queries with 1-6 components, use the type-safe {@code forEachWrapper} methods
 * which provide compile-time type safety without manual casts:</p>
 * <pre>{@code
 * // Type-safe with 4 components - no manual casts needed!
 * world.componentQuery()
 *     .withMutable(Position.class, Health.class)
 *     .withReadOnly(Velocity.class, Gravity.class)
 *     .forEachWrapper(Position.class, Health.class, Velocity.class, Gravity.class,
 *         (pos, health, vel, grav) -> {
 *             // Fully typed ComponentWrappers! IDE autocomplete works perfectly
 *             // pos, health are Mutable; vel, grav are ReadOnly based on declarations
 *             pos.get().x(); // Access values
 *             ((Mutable<Position>) pos).update(p -> new Position(
 *                 p.x() + vel.get().dx(), p.y(), p.z()));
 *         });
 * }</pre>
 *
 * <h2>Generic API (7+ components or dynamic queries)</h2>
 * <p>For queries with 7+ components, use the generic {@code forEach(Consumer<Object[]>, Class<?>...)}
 * method with manual casts:</p>
 * <pre>{@code
 * world.componentQuery()
 *     .withMutable(Position.class)
 *     .forEach(wrappers -> {
 *         Mutable<Position> pos = (Mutable<Position>) wrappers[0];
 *         pos.update(p -> new Position(p.x() + 10, p.y(), p.z()));
 *     }, Position.class);
 * }</pre>
 *
 * <h2>Read-Only Value Access</h2>
 * <p>For simple read-only iteration without wrappers, use the direct forEach overloads:</p>
 * <pre>{@code
 * // Simple read-only access - receives unwrapped values
 * world.componentQuery()
 *     .forEach(Position.class, Velocity.class, (pos, vel) -> {
 *         System.out.println("Position: " + pos.x());
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
 * 
 * <h2>Valhalla Readiness</h2>
 * This API is designed to be compatible with Project Valhalla.
 * Component wrappers can become value classes with zero code changes.
 */
public final class ComponentQuery {

    private final Collection<Archetype> archetypes;
    private final QueryCache queryCache;
    private final ComponentTypeRegistry typeRegistry;

    private final Set<Class<?>> includedTypes = new LinkedHashSet<>();
    private final Set<Class<?>> readOnlyTypes = new LinkedHashSet<>();
    private final Set<Class<?>> mutableTypes = new LinkedHashSet<>();
    private final Set<Class<?>> excludedTypes = new HashSet<>();

    /**
     * Creates a new ComponentQuery.
     * This constructor is typically called internally by EcsWorld.
     *
     * @param archetypes the collection of archetypes to query
     * @deprecated Use {@link #ComponentQuery(Collection, QueryCache, ComponentTypeRegistry)} instead
     */
    @Deprecated
    public ComponentQuery(Collection<Archetype> archetypes) {
        this(archetypes, null, null);
    }

    /**
     * Creates a new ComponentQuery with caching support.
     * This constructor is typically called internally by EcsWorld.
     *
     * @param archetypes the collection of archetypes to query
     * @param queryCache the cache for storing matching archetype results (may be null for no caching)
     * @deprecated Use {@link #ComponentQuery(Collection, QueryCache, ComponentTypeRegistry)} instead
     */
    @Deprecated
    public ComponentQuery(Collection<Archetype> archetypes, QueryCache queryCache) {
        this(archetypes, queryCache, null);
    }

    /**
     * Creates a new ComponentQuery with caching and bitset matching support.
     * This constructor is typically called internally by EcsWorld.
     *
     * @param archetypes the collection of archetypes to query
     * @param queryCache the cache for storing matching archetype results (may be null for no caching)
     * @param typeRegistry the registry for bitset-based component matching (may be null for set-based matching)
     */
    public ComponentQuery(Collection<Archetype> archetypes, QueryCache queryCache, ComponentTypeRegistry typeRegistry) {
        this.archetypes = archetypes;
        this.queryCache = queryCache;
        this.typeRegistry = typeRegistry;
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
        
        // Pre-allocate wrapper array once (reused across all entities)
        Object[] wrappers = new Object[types.length];
        
        // Pre-create wrapper instances once (reused by updating their entity index)
        // This reduces allocations from O(entities × components) to O(components)
        for (int i = 0; i < types.length; i++) {
            if (mutableTypes.contains(types[i])) {
                wrappers[i] = new Mutable<>(null, 0);
            } else {
                wrappers[i] = new ReadOnly<>(null, 0);
            }
        }
        
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
            
            // Iterate entities - reuse wrapper instances
            int size = archetype.size();
            for (int entityIdx = 0; entityIdx < size; entityIdx++) {
                // Reset wrappers to point to current entity
                for (int i = 0; i < types.length; i++) {
                    if (mutableTypes.contains(types[i])) {
                        Mutable<Object> mutable = (Mutable<Object>) wrappers[i];
                        mutable.reset((ComponentWriter<Object>) accessors[i], entityIdx);
                    } else {
                        ReadOnly<Object> readOnly = (ReadOnly<Object>) wrappers[i];
                        readOnly.reset((ComponentReader<Object>) accessors[i], entityIdx);
                    }
                }
                
                // Execute consumer - no write-back needed, Mutable writes directly
                consumer.accept(wrappers);
            }
        }
    }

    // ==================== Type-Safe ComponentWrapper forEach Methods ====================
    
    /**
     * Type-safe forEach for 1 component with ComponentWrapper access.
     * Provides compile-time type safety without manual casts.
     * The wrapper type (Mutable or ReadOnly) is determined by withMutable/withReadOnly declarations.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.componentQuery()
     *     .withMutable(Position.class)
     *     .forEach(Position.class, pos -> {
     *         // pos is typed as ComponentWrapper<Position>
     *         // Cast to Mutable<Position> if declared with withMutable
     *         ((Mutable<Position>) pos).update(p -> new Position(p.x() + 10, p.y(), p.z()));
     *     });
     * }</pre>
     *
     * @param typeA the class of the component type
     * @param consumer the consumer that receives a typed ComponentWrapper
     * @param <A> the component type
     */
    public <A> void forEachWrapper(Class<A> typeA, Consumer<ComponentWrapper<A>> consumer) {
        forEach(wrappers -> {
            @SuppressWarnings("unchecked")
            ComponentWrapper<A> a = (ComponentWrapper<A>) wrappers[0];
            consumer.accept(a);
        }, typeA);
    }

    /**
     * Type-safe forEach for 2 components with ComponentWrapper access.
     * Provides compile-time type safety without manual casts.
     * The wrapper type (Mutable or ReadOnly) is determined by withMutable/withReadOnly declarations.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.componentQuery()
     *     .withMutable(Position.class)
     *     .withReadOnly(Velocity.class)
     *     .forEach(Position.class, Velocity.class, (pos, vel) -> {
     *         // pos and vel are typed ComponentWrappers
     *         // No manual casts needed!
     *         ((Mutable<Position>) pos).update(p -> new Position(
     *             p.x() + vel.get().dx(), p.y(), p.z()));
     *     });
     * }</pre>
     *
     * @param typeA the class of the first component type
     * @param typeB the class of the second component type
     * @param consumer the consumer that receives typed ComponentWrappers
     * @param <A> the first component type
     * @param <B> the second component type
     */
    public <A, B> void forEachWrapper(Class<A> typeA, Class<B> typeB, 
                                       BiConsumer<ComponentWrapper<A>, ComponentWrapper<B>> consumer) {
        forEach(wrappers -> {
            @SuppressWarnings("unchecked")
            ComponentWrapper<A> a = (ComponentWrapper<A>) wrappers[0];
            @SuppressWarnings("unchecked")
            ComponentWrapper<B> b = (ComponentWrapper<B>) wrappers[1];
            consumer.accept(a, b);
        }, typeA, typeB);
    }

    /**
     * Type-safe forEach for 3 components with ComponentWrapper access.
     * Provides compile-time type safety without manual casts.
     * The wrapper type (Mutable or ReadOnly) is determined by withMutable/withReadOnly declarations.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.componentQuery()
     *     .withMutable(Position.class, Health.class)
     *     .withReadOnly(Velocity.class)
     *     .forEach(Position.class, Health.class, Velocity.class, (pos, health, vel) -> {
     *         // All wrappers are typed - no manual casts needed!
     *         ((Mutable<Position>) pos).update(p -> new Position(
     *             p.x() + vel.get().dx(), p.y(), p.z()));
     *     });
     * }</pre>
     *
     * @param typeA the class of the first component type
     * @param typeB the class of the second component type
     * @param typeC the class of the third component type
     * @param consumer the consumer that receives typed ComponentWrappers
     * @param <A> the first component type
     * @param <B> the second component type
     * @param <C> the third component type
     */
    public <A, B, C> void forEachWrapper(Class<A> typeA, Class<B> typeB, Class<C> typeC, 
                                          TriConsumer<ComponentWrapper<A>, ComponentWrapper<B>, 
                                                      ComponentWrapper<C>> consumer) {
        forEach(wrappers -> {
            @SuppressWarnings("unchecked")
            ComponentWrapper<A> a = (ComponentWrapper<A>) wrappers[0];
            @SuppressWarnings("unchecked")
            ComponentWrapper<B> b = (ComponentWrapper<B>) wrappers[1];
            @SuppressWarnings("unchecked")
            ComponentWrapper<C> c = (ComponentWrapper<C>) wrappers[2];
            consumer.accept(a, b, c);
        }, typeA, typeB, typeC);
    }

    /**
     * Type-safe forEach for 4 components with ComponentWrapper access.
     * Provides compile-time type safety without manual casts.
     * The wrapper type (Mutable or ReadOnly) is determined by withMutable/withReadOnly declarations.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.componentQuery()
     *     .withMutable(Position.class, Health.class)
     *     .withReadOnly(Velocity.class, Gravity.class)
     *     .forEach(Position.class, Health.class, Velocity.class, Gravity.class,
     *         (pos, health, vel, grav) -> {
     *             // Fully typed! No casts needed!
     *             ((Mutable<Position>) pos).update(p -> new Position(
     *                 p.x() + vel.get().dx(), p.y() + grav.get().force(), p.z()));
     *         });
     * }</pre>
     *
     * @param typeA the class of the first component type
     * @param typeB the class of the second component type
     * @param typeC the class of the third component type
     * @param typeD the class of the fourth component type
     * @param consumer the consumer that receives typed ComponentWrappers
     * @param <A> the first component type
     * @param <B> the second component type
     * @param <C> the third component type
     * @param <D> the fourth component type
     */
    public <A, B, C, D> void forEachWrapper(Class<A> typeA, Class<B> typeB, Class<C> typeC, 
                                             Class<D> typeD,
                                             QuadConsumer<ComponentWrapper<A>, ComponentWrapper<B>, 
                                                          ComponentWrapper<C>, ComponentWrapper<D>> consumer) {
        forEach(wrappers -> {
            @SuppressWarnings("unchecked")
            ComponentWrapper<A> a = (ComponentWrapper<A>) wrappers[0];
            @SuppressWarnings("unchecked")
            ComponentWrapper<B> b = (ComponentWrapper<B>) wrappers[1];
            @SuppressWarnings("unchecked")
            ComponentWrapper<C> c = (ComponentWrapper<C>) wrappers[2];
            @SuppressWarnings("unchecked")
            ComponentWrapper<D> d = (ComponentWrapper<D>) wrappers[3];
            consumer.accept(a, b, c, d);
        }, typeA, typeB, typeC, typeD);
    }

    /**
     * Type-safe forEach for 5 components with ComponentWrapper access.
     * Provides compile-time type safety without manual casts.
     * The wrapper type (Mutable or ReadOnly) is determined by withMutable/withReadOnly declarations.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.componentQuery()
     *     .withMutable(Position.class, Health.class)
     *     .withReadOnly(Velocity.class, Gravity.class, Weight.class)
     *     .forEach(Position.class, Health.class, Velocity.class, Gravity.class, Weight.class,
     *         (pos, health, vel, grav, weight) -> {
     *             // All 5 wrappers are typed - no manual casts needed!
     *             // Access components with get() and modify with set()/update()
     *         });
     * }</pre>
     *
     * @param typeA the class of the first component type
     * @param typeB the class of the second component type
     * @param typeC the class of the third component type
     * @param typeD the class of the fourth component type
     * @param typeE the class of the fifth component type
     * @param consumer the consumer that receives typed ComponentWrappers
     * @param <A> the first component type
     * @param <B> the second component type
     * @param <C> the third component type
     * @param <D> the fourth component type
     * @param <E> the fifth component type
     */
    public <A, B, C, D, E> void forEachWrapper(Class<A> typeA, Class<B> typeB, Class<C> typeC, 
                                                Class<D> typeD, Class<E> typeE,
                                                PentaConsumer<ComponentWrapper<A>, ComponentWrapper<B>, 
                                                              ComponentWrapper<C>, ComponentWrapper<D>,
                                                              ComponentWrapper<E>> consumer) {
        forEach(wrappers -> {
            @SuppressWarnings("unchecked")
            ComponentWrapper<A> a = (ComponentWrapper<A>) wrappers[0];
            @SuppressWarnings("unchecked")
            ComponentWrapper<B> b = (ComponentWrapper<B>) wrappers[1];
            @SuppressWarnings("unchecked")
            ComponentWrapper<C> c = (ComponentWrapper<C>) wrappers[2];
            @SuppressWarnings("unchecked")
            ComponentWrapper<D> d = (ComponentWrapper<D>) wrappers[3];
            @SuppressWarnings("unchecked")
            ComponentWrapper<E> e = (ComponentWrapper<E>) wrappers[4];
            consumer.accept(a, b, c, d, e);
        }, typeA, typeB, typeC, typeD, typeE);
    }

    /**
     * Type-safe forEach for 6 components with ComponentWrapper access.
     * Provides compile-time type safety without manual casts.
     * The wrapper type (Mutable or ReadOnly) is determined by withMutable/withReadOnly declarations.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.componentQuery()
     *     .withMutable(Position.class, Health.class, Armor.class)
     *     .withReadOnly(Velocity.class, Gravity.class, Weight.class)
     *     .forEach(Position.class, Health.class, Armor.class, 
     *              Velocity.class, Gravity.class, Weight.class,
     *         (pos, health, armor, vel, grav, weight) -> {
     *             // All 6 wrappers are typed - no manual casts needed!
     *             // For 7+ components, use the generic forEach(Consumer<Object[]>, Class<?>...) method
     *         });
     * }</pre>
     *
     * @param typeA the class of the first component type
     * @param typeB the class of the second component type
     * @param typeC the class of the third component type
     * @param typeD the class of the fourth component type
     * @param typeE the class of the fifth component type
     * @param typeF the class of the sixth component type
     * @param consumer the consumer that receives typed ComponentWrappers
     * @param <A> the first component type
     * @param <B> the second component type
     * @param <C> the third component type
     * @param <D> the fourth component type
     * @param <E> the fifth component type
     * @param <F> the sixth component type
     */
    public <A, B, C, D, E, F> void forEachWrapper(Class<A> typeA, Class<B> typeB, Class<C> typeC, 
                                                   Class<D> typeD, Class<E> typeE, Class<F> typeF,
                                                   HexaConsumer<ComponentWrapper<A>, ComponentWrapper<B>, 
                                                                ComponentWrapper<C>, ComponentWrapper<D>,
                                                                ComponentWrapper<E>, ComponentWrapper<F>> consumer) {
        forEach(wrappers -> {
            @SuppressWarnings("unchecked")
            ComponentWrapper<A> a = (ComponentWrapper<A>) wrappers[0];
            @SuppressWarnings("unchecked")
            ComponentWrapper<B> b = (ComponentWrapper<B>) wrappers[1];
            @SuppressWarnings("unchecked")
            ComponentWrapper<C> c = (ComponentWrapper<C>) wrappers[2];
            @SuppressWarnings("unchecked")
            ComponentWrapper<D> d = (ComponentWrapper<D>) wrappers[3];
            @SuppressWarnings("unchecked")
            ComponentWrapper<E> e = (ComponentWrapper<E>) wrappers[4];
            @SuppressWarnings("unchecked")
            ComponentWrapper<F> f = (ComponentWrapper<F>) wrappers[5];
            consumer.accept(a, b, c, d, e, f);
        }, typeA, typeB, typeC, typeD, typeE, typeF);
    }

    // ==================== Mutable Access Helper Methods ====================
    
    /**
     * Iterates over all matching entities with mutable access to a single component.
     * The component is automatically marked as mutable for this query.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.componentQuery()
     *     .forEachMutable(Health.class, h -> {
     *         if (h.get().current() < h.get().max()) {
     *             h.set(new Health(h.get().current() + 1, h.get().max()));
     *         }
     *     });
     * }</pre>
     *
     * @param typeA the class of the component type
     * @param consumer the consumer for the mutable wrapper
     * @param <A> the component type
     */
    @SuppressWarnings("unchecked")
    public <A> void forEachMutable(Class<A> typeA, Consumer<Mutable<A>> consumer) {
        withMutable(typeA);
        forEach(wrappers -> {
            Mutable<A> a = (Mutable<A>) wrappers[0];
            consumer.accept(a);
        }, typeA);
    }

    /**
     * Iterates over all matching entities with access control for two components.
     * The first component is mutable, the second is read-only.
     * Components are automatically marked based on their wrapper types.
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * world.componentQuery()
     *     .forEachWithAccess(Position.class, Velocity.class, (pos, vel) -> {
     *         pos.set(new Position(
     *             pos.get().x() + vel.get().dx(),
     *             pos.get().y() + vel.get().dy(),
     *             pos.get().z() + vel.get().dz()
     *         ));
     *     });
     * }</pre>
     *
     * @param typeA the class of the first (mutable) component type
     * @param typeB the class of the second (read-only) component type
     * @param consumer the consumer for the component wrappers
     * @param <A> the first component type
     * @param <B> the second component type
     */
    @SuppressWarnings("unchecked")
    public <A, B> void forEachWithAccess(Class<A> typeA, Class<B> typeB, 
                                          BiConsumer<Mutable<A>, ReadOnly<B>> consumer) {
        withMutable(typeA);
        withReadOnly(typeB);
        forEach(wrappers -> {
            Mutable<A> a = (Mutable<A>) wrappers[0];
            ReadOnly<B> b = (ReadOnly<B>) wrappers[1];
            consumer.accept(a, b);
        }, typeA, typeB);
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
     * 
     * <p>Results are cached for improved performance. The cache is automatically
     * invalidated when new archetypes are created in the EcsWorld.</p>
     */
    private List<Archetype> getMatchingArchetypes(Set<Class<?>> additionalRequired) {
        // Try to get from cache first
        if (queryCache != null) {
            QueryCacheKey key = new QueryCacheKey(includedTypes, excludedTypes, additionalRequired);
            List<Archetype> cached = queryCache.get(key);
            if (cached != null) {
                return cached;
            }
            
            // Not in cache - compute and store
            List<Archetype> matching = computeMatchingArchetypes(additionalRequired);
            queryCache.put(key, matching);
            return matching;
        }
        
        // No cache - compute directly
        return computeMatchingArchetypes(additionalRequired);
    }
    
    /**
     * Computes matching archetypes without caching.
     * Uses bitset-based matching when a type registry is available for O(1) checks,
     * otherwise falls back to set-based matching.
     */
    private List<Archetype> computeMatchingArchetypes(Set<Class<?>> additionalRequired) {
        // Use bitset matching if registry is available
        if (typeRegistry != null) {
            return computeMatchingArchetypesWithBitsets(additionalRequired);
        }
        
        // Fallback to set-based matching
        return computeMatchingArchetypesWithSets(additionalRequired);
    }
    
    /**
     * Computes matching archetypes using O(1) bitset operations.
     */
    private List<Archetype> computeMatchingArchetypesWithBitsets(Set<Class<?>> additionalRequired) {
        List<Archetype> matching = new ArrayList<>();
        
        // Pre-compute required and excluded bitsets
        BitSet requiredBits = typeRegistry.toBitSet(includedTypes);
        BitSet additionalBits = typeRegistry.toBitSet(additionalRequired);
        requiredBits.or(additionalBits);
        
        BitSet excludedBits = typeRegistry.toBitSet(excludedTypes);
        
        for (Archetype archetype : archetypes) {
            BitSet archetypeBits = typeRegistry.toBitSet(archetype.getComponentTypes());
            
            // O(1) inclusion check: (archetypeBits & requiredBits) == requiredBits
            if (!ComponentTypeRegistry.containsAll(archetypeBits, requiredBits)) {
                continue;
            }
            
            // O(1) exclusion check: (archetypeBits & excludedBits) == 0
            if (ComponentTypeRegistry.intersects(archetypeBits, excludedBits)) {
                continue;
            }
            
            matching.add(archetype);
        }
        
        return matching;
    }
    
    /**
     * Computes matching archetypes using set operations (fallback).
     */
    private List<Archetype> computeMatchingArchetypesWithSets(Set<Class<?>> additionalRequired) {
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
