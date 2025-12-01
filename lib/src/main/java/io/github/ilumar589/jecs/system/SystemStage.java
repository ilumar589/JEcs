package io.github.ilumar589.jecs.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a group of systems that can safely run in parallel.
 * All systems in a stage have no conflicting component access patterns.
 *
 * <h2>Parallel Execution</h2>
 * Systems within a stage can be executed concurrently because:
 * <ul>
 *   <li>No two systems write to the same component type</li>
 *   <li>No system writes to a component that another system reads</li>
 * </ul>
 *
 * <h2>Stage Ordering</h2>
 * Stages are executed sequentially, ensuring all systems in one stage
 * complete before systems in the next stage begin.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Stage 0: Physics and AI can run in parallel
 * SystemStage stage0 = new SystemStage(List.of(physicsSystem, aiSystem));
 *
 * // Stage 1: Rendering depends on physics results
 * SystemStage stage1 = new SystemStage(List.of(renderSystem));
 * }</pre>
 */
public final class SystemStage {

    private final List<System> systems;

    /**
     * Creates a new stage with the given systems.
     *
     * @param systems the systems in this stage
     */
    public SystemStage(List<System> systems) {
        this.systems = Collections.unmodifiableList(new ArrayList<>(systems));
    }

    /**
     * Returns the systems in this stage.
     *
     * @return unmodifiable list of systems
     */
    public List<System> getSystems() {
        return systems;
    }

    /**
     * Returns the number of systems in this stage.
     *
     * @return the system count
     */
    public int size() {
        return systems.size();
    }

    /**
     * Checks if this stage is empty.
     *
     * @return true if the stage has no systems
     */
    public boolean isEmpty() {
        return systems.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SystemStage{systems=[");
        for (int i = 0; i < systems.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(systems.get(i).getName());
        }
        sb.append("]}");
        return sb.toString();
    }
}
