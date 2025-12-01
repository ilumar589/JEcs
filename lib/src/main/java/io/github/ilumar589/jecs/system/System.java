package io.github.ilumar589.jecs.system;

import io.github.ilumar589.jecs.query.ComponentQuery;
import io.github.ilumar589.jecs.world.EcsWorld;

import java.util.function.BiConsumer;

/**
 * Represents an ECS system that operates on entities with specific component patterns.
 * Systems declare their component access patterns (read-only, mutable, excluded) and
 * provide an execution function that processes matching entities.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * System physics = new System.Builder("Physics")
 *     .withMutable(Position.class)
 *     .withReadOnly(Velocity.class)
 *     .execute((world, query) -> {
 *         query.forEachWithAccess(Position.class, Velocity.class, (pos, vel) -> {
 *             Position current = pos.get();
 *             Velocity velocity = vel.get();
 *             pos.set(new Position(
 *                 current.x() + velocity.dx(),
 *                 current.y() + velocity.dy(),
 *                 current.z() + velocity.dz()
 *             ));
 *         });
 *     })
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * System instances are immutable and thread-safe. The execution function may be
 * called concurrently from multiple threads if the system is in a parallel stage.
 * The execution function should ensure thread-safe access to any shared state.
 */
public final class System {

    private final String name;
    private final ComponentAccess access;
    private final BiConsumer<EcsWorld, ComponentQuery> executor;

    private System(String name, ComponentAccess access, BiConsumer<EcsWorld, ComponentQuery> executor) {
        this.name = name;
        this.access = access;
        this.executor = executor;
    }

    /**
     * Returns the name of this system.
     *
     * @return the system name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the component access requirements for this system.
     *
     * @return the component access pattern
     */
    public ComponentAccess getAccess() {
        return access;
    }

    /**
     * Executes this system on the given world.
     * Creates a query based on the system's access requirements and invokes the executor.
     *
     * @param world the ECS world to operate on
     */
    public void execute(EcsWorld world) {
        ComponentQuery query = world.componentQuery();
        
        // Configure query based on access requirements
        for (Class<?> type : access.getReadOnly()) {
            query.withReadOnly(type);
        }
        for (Class<?> type : access.getMutable()) {
            query.withMutable(type);
        }
        for (Class<?> type : access.getExcluded()) {
            query.without(type);
        }
        
        executor.accept(world, query);
    }

    /**
     * Checks if this system conflicts with another system.
     * Two systems conflict if their component access patterns conflict.
     *
     * @param other the other system
     * @return true if the systems conflict
     */
    public boolean conflictsWith(System other) {
        return access.conflictsWith(other.access);
    }

    @Override
    public String toString() {
        return "System{name='" + name + "', access=" + access + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        System system = (System) obj;
        return name.equals(system.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Builder for creating System instances.
     */
    public static final class Builder {
        private final String name;
        private final ComponentAccess.Builder accessBuilder = new ComponentAccess.Builder();
        private BiConsumer<EcsWorld, ComponentQuery> executor;

        /**
         * Creates a new System builder with the given name.
         *
         * @param name the system name
         * @throws IllegalArgumentException if name is null or empty
         */
        public Builder(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("System name cannot be null or empty");
            }
            this.name = name;
        }

        /**
         * Adds a component type with read-only access.
         *
         * @param type the component type
         * @return this builder for chaining
         */
        public Builder withReadOnly(Class<?> type) {
            accessBuilder.addReadOnly(type);
            return this;
        }

        /**
         * Adds multiple component types with read-only access.
         *
         * @param types the component types
         * @return this builder for chaining
         */
        @SafeVarargs
        public final Builder withReadOnly(Class<?>... types) {
            accessBuilder.addReadOnly(types);
            return this;
        }

        /**
         * Adds a component type with mutable access.
         *
         * @param type the component type
         * @return this builder for chaining
         */
        public Builder withMutable(Class<?> type) {
            accessBuilder.addMutable(type);
            return this;
        }

        /**
         * Adds multiple component types with mutable access.
         *
         * @param types the component types
         * @return this builder for chaining
         */
        @SafeVarargs
        public final Builder withMutable(Class<?>... types) {
            accessBuilder.addMutable(types);
            return this;
        }

        /**
         * Adds a component type to exclude.
         *
         * @param type the component type to exclude
         * @return this builder for chaining
         */
        public Builder without(Class<?> type) {
            accessBuilder.addExcluded(type);
            return this;
        }

        /**
         * Adds multiple component types to exclude.
         *
         * @param types the component types to exclude
         * @return this builder for chaining
         */
        @SafeVarargs
        public final Builder without(Class<?>... types) {
            accessBuilder.addExcluded(types);
            return this;
        }

        /**
         * Sets the execution function for this system.
         * The function receives the ECS world and a pre-configured query matching
         * the system's access requirements.
         *
         * @param executor the execution function
         * @return this builder for chaining
         */
        public Builder execute(BiConsumer<EcsWorld, ComponentQuery> executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Builds the System instance.
         *
         * @return the built System
         * @throws IllegalStateException if no executor was set
         */
        public System build() {
            if (executor == null) {
                throw new IllegalStateException("System executor must be set");
            }
            return new System(name, accessBuilder.build(), executor);
        }
    }
}
