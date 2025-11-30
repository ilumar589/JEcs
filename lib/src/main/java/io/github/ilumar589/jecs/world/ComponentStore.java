package io.github.ilumar589.jecs.world;

import io.github.ilumar589.jecs.component.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A type-safe, array-based storage for components of a single type.
 * This class provides cache-friendly storage by using contiguous arrays instead of
 * boxed object references through interface indirection.
 * 
 * <h2>Design Philosophy</h2>
 * This implementation uses {@code Object[]} internally for storage, but maintains 
 * type safety through generics at the API level. This approach:
 * <ul>
 *   <li>Reduces pointer indirection compared to {@code List<Component>}</li>
 *   <li>Provides contiguous memory layout for better cache locality</li>
 *   <li>Pre-allocates capacity to reduce reallocations during entity additions</li>
 *   <li>Prepares for Project Valhalla value types migration</li>
 * </ul>
 * 
 * <h2>Project Valhalla Migration Path</h2>
 * When Project Valhalla lands with value types (inline types), this class can be
 * updated to:
 * <ul>
 *   <li>Use specialized primitive arrays for primitive-like value types</li>
 *   <li>Take advantage of flattened object layouts where components are stored 
 *       inline rather than as references</li>
 *   <li>Implement {@code value class} semantics for the store itself</li>
 * </ul>
 * 
 * <h2>Cache-Friendly Iteration Pattern</h2>
 * For optimal cache performance, iterate over components using:
 * <pre>{@code
 * Object[] data = store.getRawData();
 * int size = store.size();
 * for (int i = 0; i < size; i++) {
 *     T component = (T) data[i];
 *     // Process component
 * }
 * }</pre>
 * This avoids iterator overhead and keeps accesses sequential.
 *
 * @param <T> the component type stored in this column
 */
public final class ComponentStore<T extends Component> {
    
    /**
     * Default initial capacity for new component stores.
     * Chosen to balance memory overhead against reallocation frequency.
     * A typical ECS archetype may have 16-64 entities initially.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    
    /**
     * Growth factor when resizing the array.
     * 1.5x provides good amortized performance while being more memory-efficient than 2x.
     */
    private static final double GROWTH_FACTOR = 1.5;
    
    /**
     * The underlying component storage array.
     * 
     * <h3>Valhalla Note</h3>
     * When value types arrive, this could be replaced with a specialized array
     * of the value type, eliminating the reference indirection:
     * <pre>
     * // Future Valhalla syntax (hypothetical):
     * value T[] data;  // Flattened storage without object headers
     * </pre>
     */
    private Object[] data;
    
    /**
     * Current number of components stored.
     * Always less than or equal to {@code data.length}.
     */
    private int size;
    
    /**
     * The component type stored in this column.
     * Retained for type safety and debugging.
     */
    private final Class<T> componentType;
    
    /**
     * Creates a new ComponentStore with the default initial capacity.
     *
     * @param componentType the Class object representing the component type
     */
    public ComponentStore(Class<T> componentType) {
        this(componentType, DEFAULT_INITIAL_CAPACITY);
    }
    
    /**
     * Creates a new ComponentStore with a specified initial capacity.
     * Use this constructor when the expected number of entities is known
     * to reduce reallocations.
     *
     * @param componentType the Class object representing the component type
     * @param initialCapacity the initial array capacity (minimum 1)
     */
    public ComponentStore(Class<T> componentType, int initialCapacity) {
        this.componentType = componentType;
        this.data = new Object[Math.max(1, initialCapacity)];
        this.size = 0;
    }
    
    /**
     * Adds a component to the store.
     * If the internal array is full, it is resized using the growth factor.
     *
     * @param component the component to add
     * @return the index at which the component was stored
     */
    public int add(T component) {
        ensureCapacity(size + 1);
        int index = size;
        data[size++] = component;
        return index;
    }
    
    /**
     * Gets a component at the specified index.
     *
     * @param index the index of the component
     * @return the component at the index
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return (T) data[index];
    }
    
    /**
     * Sets a component at the specified index.
     *
     * @param index the index at which to set the component
     * @param component the component to set
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    public void set(int index, T component) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        data[index] = component;
    }
    
    /**
     * Removes the component at the specified index using swap-and-pop.
     * This maintains contiguous storage and O(1) removal but does not preserve order.
     *
     * @param index the index of the component to remove
     * @return the removed component
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    @SuppressWarnings("unchecked")
    public T removeSwapPop(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        
        T removed = (T) data[index];
        int lastIndex = size - 1;
        
        if (index != lastIndex) {
            // Swap with last element
            data[index] = data[lastIndex];
        }
        
        // Clear the last slot and decrement size
        data[lastIndex] = null;
        size--;
        
        return removed;
    }
    
    /**
     * Returns the current number of components stored.
     *
     * @return the size of the store
     */
    public int size() {
        return size;
    }
    
    /**
     * Returns the current capacity of the underlying array.
     *
     * @return the capacity
     */
    public int capacity() {
        return data.length;
    }
    
    /**
     * Returns the component type stored in this column.
     *
     * @return the component type class
     */
    public Class<T> getComponentType() {
        return componentType;
    }
    
    /**
     * Returns an unmodifiable list view of the components.
     * Note: This creates a new list wrapper on each call. For performance-critical
     * iteration, use {@link #getRawData()} with the size.
     *
     * @return an unmodifiable list containing all components
     */
    @SuppressWarnings("unchecked")
    public List<T> asList() {
        if (size == 0) {
            return Collections.emptyList();
        }
        // Create a properly sized copy to return
        Object[] copy = Arrays.copyOf(data, size);
        return Collections.unmodifiableList((List<T>) Arrays.asList(copy));
    }
    
    /**
     * Returns the raw data array for direct access.
     * 
     * <h3>Warning</h3>
     * This method exposes internal state for performance-critical iteration.
     * Callers must:
     * <ul>
     *   <li>Only read elements from index 0 to {@code size() - 1}</li>
     *   <li>Not modify the array contents</li>
     *   <li>Not retain the array reference across structural modifications</li>
     * </ul>
     *
     * <h3>Cache-Friendly Usage</h3>
     * <pre>{@code
     * Object[] rawData = store.getRawData();
     * int count = store.size();
     * for (int i = 0; i < count; i++) {
     *     MyComponent c = (MyComponent) rawData[i];
     *     // Sequential access maximizes cache hits
     * }
     * }</pre>
     *
     * @return the internal data array
     */
    public Object[] getRawData() {
        return data;
    }
    
    /**
     * Ensures the internal array has capacity for at least the specified number of elements.
     *
     * @param minCapacity the minimum required capacity
     */
    private void ensureCapacity(int minCapacity) {
        if (minCapacity > data.length) {
            int newCapacity = (int) Math.max(minCapacity, data.length * GROWTH_FACTOR);
            data = Arrays.copyOf(data, newCapacity);
        }
    }
    
    /**
     * Trims the internal array to the current size.
     * Call this after bulk additions are complete to release unused memory.
     */
    public void trimToSize() {
        if (size < data.length) {
            data = Arrays.copyOf(data, size);
        }
    }
    
    @Override
    public String toString() {
        return "ComponentStore{" +
                "type=" + componentType.getSimpleName() +
                ", size=" + size +
                ", capacity=" + data.length +
                '}';
    }
}
