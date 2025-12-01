package io.github.ilumar589.jecs.system;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Describes the component access requirements for a system.
 * Used for conflict detection to determine which systems can run in parallel.
 *
 * <h2>Access Types</h2>
 * <ul>
 *   <li><b>Read-only:</b> Components that are only read, not modified</li>
 *   <li><b>Mutable:</b> Components that may be modified</li>
 *   <li><b>Excluded:</b> Components that must not be present on entities</li>
 * </ul>
 *
 * <h2>Conflict Rules</h2>
 * Two ComponentAccess instances have a conflict if:
 * <ul>
 *   <li>Both mutate the same component type</li>
 *   <li>One mutates a component that the other reads</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ComponentAccess physics = new ComponentAccess.Builder()
 *     .addReadOnly(Velocity.class)
 *     .addMutable(Position.class)
 *     .build();
 *
 * ComponentAccess render = new ComponentAccess.Builder()
 *     .addReadOnly(Position.class)
 *     .addReadOnly(Sprite.class)
 *     .build();
 *
 * // These don't conflict: physics writes Position, render only reads it
 * // But if render also wrote Position, they would conflict
 * boolean conflicts = physics.conflictsWith(render); // false
 * }</pre>
 */
public final class ComponentAccess {

    private final Set<Class<?>> readOnly;
    private final Set<Class<?>> mutable;
    private final Set<Class<?>> excluded;

    private ComponentAccess(Set<Class<?>> readOnly, Set<Class<?>> mutable, Set<Class<?>> excluded) {
        this.readOnly = Collections.unmodifiableSet(new HashSet<>(readOnly));
        this.mutable = Collections.unmodifiableSet(new HashSet<>(mutable));
        this.excluded = Collections.unmodifiableSet(new HashSet<>(excluded));
    }

    /**
     * Returns the set of component types with read-only access.
     *
     * @return unmodifiable set of read-only component types
     */
    public Set<Class<?>> getReadOnly() {
        return readOnly;
    }

    /**
     * Returns the set of component types with mutable access.
     *
     * @return unmodifiable set of mutable component types
     */
    public Set<Class<?>> getMutable() {
        return mutable;
    }

    /**
     * Returns the set of excluded component types.
     *
     * @return unmodifiable set of excluded component types
     */
    public Set<Class<?>> getExcluded() {
        return excluded;
    }

    /**
     * Returns all component types that are accessed (read-only or mutable).
     *
     * @return unmodifiable set of all accessed component types
     */
    public Set<Class<?>> getAllAccessed() {
        Set<Class<?>> all = new HashSet<>(readOnly);
        all.addAll(mutable);
        return Collections.unmodifiableSet(all);
    }

    /**
     * Checks if this access pattern conflicts with another.
     * A conflict occurs when:
     * <ul>
     *   <li>Both access patterns mutate the same component type</li>
     *   <li>One mutates a component that the other reads or mutates</li>
     * </ul>
     *
     * @param other the other component access to check against
     * @return true if there is a conflict
     */
    public boolean conflictsWith(ComponentAccess other) {
        // Check if both mutate the same component
        for (Class<?> type : mutable) {
            if (other.mutable.contains(type)) {
                return true;
            }
        }

        // Check if this mutates what other reads or mutates
        for (Class<?> type : mutable) {
            if (other.readOnly.contains(type)) {
                return true;
            }
        }

        // Check if other mutates what this reads
        for (Class<?> type : other.mutable) {
            if (readOnly.contains(type)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if this access pattern has any read-only components.
     *
     * @return true if there are read-only components
     */
    public boolean hasReadOnly() {
        return !readOnly.isEmpty();
    }

    /**
     * Checks if this access pattern has any mutable components.
     *
     * @return true if there are mutable components
     */
    public boolean hasMutable() {
        return !mutable.isEmpty();
    }

    /**
     * Checks if this access pattern has any excluded components.
     *
     * @return true if there are excluded components
     */
    public boolean hasExcluded() {
        return !excluded.isEmpty();
    }

    @Override
    public String toString() {
        return "ComponentAccess{readOnly=" + readOnly + ", mutable=" + mutable + ", excluded=" + excluded + "}";
    }

    /**
     * Builder for creating ComponentAccess instances.
     */
    public static final class Builder {
        private final Set<Class<?>> readOnly = new HashSet<>();
        private final Set<Class<?>> mutable = new HashSet<>();
        private final Set<Class<?>> excluded = new HashSet<>();

        /**
         * Adds a component type with read-only access.
         *
         * @param type the component type
         * @return this builder for chaining
         */
        public Builder addReadOnly(Class<?> type) {
            readOnly.add(type);
            return this;
        }

        /**
         * Adds multiple component types with read-only access.
         *
         * @param types the component types
         * @return this builder for chaining
         */
        @SafeVarargs
        public final Builder addReadOnly(Class<?>... types) {
            for (Class<?> type : types) {
                readOnly.add(type);
            }
            return this;
        }

        /**
         * Adds a component type with mutable access.
         *
         * @param type the component type
         * @return this builder for chaining
         */
        public Builder addMutable(Class<?> type) {
            mutable.add(type);
            return this;
        }

        /**
         * Adds multiple component types with mutable access.
         *
         * @param types the component types
         * @return this builder for chaining
         */
        @SafeVarargs
        public final Builder addMutable(Class<?>... types) {
            for (Class<?> type : types) {
                mutable.add(type);
            }
            return this;
        }

        /**
         * Adds a component type to exclude.
         *
         * @param type the component type to exclude
         * @return this builder for chaining
         */
        public Builder addExcluded(Class<?> type) {
            excluded.add(type);
            return this;
        }

        /**
         * Adds multiple component types to exclude.
         *
         * @param types the component types to exclude
         * @return this builder for chaining
         */
        @SafeVarargs
        public final Builder addExcluded(Class<?>... types) {
            for (Class<?> type : types) {
                excluded.add(type);
            }
            return this;
        }

        /**
         * Builds the ComponentAccess instance.
         *
         * @return the built ComponentAccess
         */
        public ComponentAccess build() {
            return new ComponentAccess(readOnly, mutable, excluded);
        }
    }
}
