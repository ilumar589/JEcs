package io.github.ilumar589.jecs.world;

import io.github.ilumar589.jecs.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.SoftReference;
import java.lang.reflect.RecordComponent;
import java.util.*;

/**
 * An archetype implementation that decomposes record components into their primitive
 * fields for improved cache locality, SIMD vectorization, and reduced memory overhead.
 * 
 * <h2>Storage Model</h2>
 * Each primitive field is stored in its own typed array:
 * <pre>
 * Archetype storage for Position(float x, float y, float z):
 *   float[] x -> [1, 4, ...]  // Direct primitive storage
 *   float[] y -> [2, 5, ...]
 *   float[] z -> [3, 6, ...]
 * </pre>
 * 
 * <h2>Performance Benefits</h2>
 * <ul>
 *   <li><b>Cache locality:</b> Sequential access to same-typed fields</li>
 *   <li><b>SIMD potential:</b> JIT can vectorize primitive array operations</li>
 *   <li><b>Memory reduction:</b> No object headers for primitive fields</li>
 *   <li><b>Zero GC:</b> Primitive arrays don't create garbage during iteration</li>
 * </ul>
 * 
 * <h2>Requirements</h2>
 * Components must be records with supported primitive field types:
 * <ul>
 *   <li>int, float, double, long, boolean, byte, short, char</li>
 *   <li>String (treated as a reference type but still decomposed)</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <ul>
 *   <li>Readers use plain reads (no barriers) - maximum performance</li>
 *   <li>Writers use release semantics - safe publication</li>
 *   <li>External synchronization needed for concurrent structural modifications</li>
 * </ul>
 * 
 * @see ComponentReader
 * @see ComponentWriter
 */
public final class Archetype {
    /**
     * Default initial capacity for entity storage.
     * Using 64 provides better cache alignment and reduces reallocations
     * for typical ECS workloads where archetypes often contain many entities.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 64;
    
    /**
     * Minimum initial capacity for adaptive sizing.
     * Uses smaller initial capacity for memory efficiency.
     */
    private static final int MIN_INITIAL_CAPACITY = 8;
    
    /**
     * Growth factor when resizing arrays.
     * Using 2.0 provides power-of-2 sizes which have better cache alignment
     * and more predictable memory patterns.
     */
    private static final double GROWTH_FACTOR = 2.0;
    
    /**
     * Thread-local storage for component read arguments.
     * Reduces GC pressure by reusing Object[] arrays across component reads.
     * The map key is the array size (number of fields), value is the reusable array.
     */
    private static final ThreadLocal<Map<Integer, Object[]>> THREAD_LOCAL_ARGS = 
        ThreadLocal.withInitial(HashMap::new);

    // Global typed arrays - ONE per primitive type, using GlobalArray
    private final Map<Class<?>, GlobalArray> globalArrays;

    private final Set<Class<?>> componentTypes;
    private final Map<FieldKey, FieldColumn> fieldColumns;
    private final Map<Class<?>, List<FieldColumn>> componentFieldColumns;
    private final Map<Class<?>, MethodHandle> componentConstructors;
    
    // Cached readers and writers with SoftReference for memory efficiency.
    // SoftReferences allow GC to reclaim memory under pressure while
    // keeping caches alive when memory is plentiful.
    private final Map<Class<?>, SoftReference<ComponentReader<?>>> cachedReaders;
    private final Map<Class<?>, SoftReference<ComponentWriter<?>>> cachedWriters;
    
    private final List<Entity> entities;
    private final Map<Entity, Integer> entityToIndex;
    
    private int capacity;
    private int size;

    /**
     * Creates a new Archetype for the given component types.
     * Uses adaptive initial capacity (minimum capacity) to reduce memory
     * overhead for archetypes that may contain few entities.
     * The capacity will grow automatically as entities are added.
     *
     * @param componentTypes the set of component types
     * @throws IllegalArgumentException if any type is not a record or has unsupported fields
     */
    public Archetype(Set<Class<?>> componentTypes) {
        this(componentTypes, MIN_INITIAL_CAPACITY);
    }

    /**
     * Creates a new Archetype with a specified initial capacity.
     *
     * @param componentTypes the set of component types
     * @param initialCapacity the initial storage capacity
     * @throws IllegalArgumentException if any type is not a record or has unsupported fields
     */
    public Archetype(Set<Class<?>> componentTypes, int initialCapacity) {
        this.componentTypes = Set.copyOf(componentTypes);
        this.capacity = Math.max(MIN_INITIAL_CAPACITY, initialCapacity);
        this.size = 0;
        
        this.fieldColumns = new HashMap<>();
        this.componentFieldColumns = new HashMap<>();
        this.componentConstructors = new HashMap<>();
        this.cachedReaders = new HashMap<>();
        this.cachedWriters = new HashMap<>();
        this.entities = new ArrayList<>(this.capacity);
        this.entityToIndex = new HashMap<>();
        this.globalArrays = new HashMap<>();
        
        // Count fields per type to allocate arrays
        Map<Class<?>, Integer> fieldCounts = new HashMap<>();
        
        for (Class<?> type : componentTypes) {
            if (!type.isRecord()) {
                throw new IllegalArgumentException("Component type must be a record: " + type.getName());
            }
            
            RecordComponent[] components = type.getRecordComponents();
            for (RecordComponent rc : components) {
                Class<?> fieldClass = rc.getType();
                if (!GlobalArray.isSupported(fieldClass)) {
                    throw new IllegalArgumentException(
                        "Unsupported field type: " + fieldClass.getName() + 
                        " for field " + rc.getName() + " in " + type.getName());
                }
                
                // Normalize to primitive class for counting
                Class<?> normalizedClass = normalizeFieldClass(fieldClass);
                fieldCounts.merge(normalizedClass, 1, Integer::sum);
            }
        }
        
        // Allocate global arrays for each type that has fields
        for (Map.Entry<Class<?>, Integer> entry : fieldCounts.entrySet()) {
            Class<?> fieldClass = entry.getKey();
            int fieldCount = entry.getValue();
            GlobalArray array = GlobalArray.fromClass(fieldClass, fieldCount * capacity);
            if (array != null) {
                globalArrays.put(fieldClass, array);
            }
        }
        
        // Track allocation offsets per type
        Map<Class<?>, Integer> offsets = new HashMap<>();
        for (Class<?> fieldClass : fieldCounts.keySet()) {
            offsets.put(fieldClass, 0);
        }
        
        // Create field columns for each component type
        for (Class<?> type : componentTypes) {
            List<FieldColumn> columns = new ArrayList<>();
            RecordComponent[] components = type.getRecordComponents();
            
            for (int i = 0; i < components.length; i++) {
                RecordComponent rc = components[i];
                Class<?> normalizedClass = normalizeFieldClass(rc.getType());
                
                int offset = offsets.get(normalizedClass);
                FieldColumn column = new FieldColumn(type, rc, i, offset);
                
                fieldColumns.put(column.getFieldKey(), column);
                columns.add(column);
                
                // Update offset for next field of this type
                offsets.put(normalizedClass, offset + capacity);
            }
            
            componentFieldColumns.put(type, columns);
            componentConstructors.put(type, createConstructorHandle(type, components));
        }
    }
    
    /**
     * Normalizes a field class to its primitive form.
     * Boxed types (Integer, Float, etc.) are converted to their primitive equivalents.
     */
    private Class<?> normalizeFieldClass(Class<?> fieldClass) {
        if (fieldClass == Integer.class) return int.class;
        if (fieldClass == Float.class) return float.class;
        if (fieldClass == Double.class) return double.class;
        if (fieldClass == Long.class) return long.class;
        if (fieldClass == Boolean.class) return boolean.class;
        if (fieldClass == Byte.class) return byte.class;
        if (fieldClass == Short.class) return short.class;
        if (fieldClass == Character.class) return char.class;
        return fieldClass;
    }

    /**
     * Creates a method handle for the canonical constructor of a record type.
     */
    private MethodHandle createConstructorHandle(Class<?> recordType, RecordComponent[] components) {
        try {
            Class<?>[] paramTypes = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                paramTypes[i] = components[i].getType();
            }
            return MethodHandles.lookup()
                    .findConstructor(recordType, MethodType.methodType(void.class, paramTypes));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Cannot access constructor for " + recordType.getName(), e);
        }
    }

    /**
     * Returns the set of component types in this archetype.
     *
     * @return an unmodifiable set of component types
     */
    public Set<Class<?>> getComponentTypes() {
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
        return size;
    }

    /**
     * Returns the current capacity of the archetype.
     *
     * @return the capacity
     */
    public int capacity() {
        return capacity;
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
     * @param components the components (must match archetype's component types)
     * @throws IllegalArgumentException if entity exists or components don't match
     */
    public void addEntity(Entity entity, Map<Class<?>, Object> components) {
        if (entityToIndex.containsKey(entity)) {
            throw new IllegalArgumentException("Entity already exists in archetype: " + entity);
        }
        
        if (!components.keySet().equals(componentTypes)) {
            throw new IllegalArgumentException("Component types don't match archetype. Expected: " 
                    + componentTypes + ", got: " + components.keySet());
        }
        
        ensureCapacity(size + 1);
        
        int index = size;
        entities.add(entity);
        entityToIndex.put(entity, index);
        
        // Write all component fields to global arrays
        for (var entry : components.entrySet()) {
            Class<?> type = entry.getKey();
            Object component = entry.getValue();
            List<FieldColumn> columns = componentFieldColumns.get(type);
            
            for (FieldColumn column : columns) {
                GlobalArray array = getGlobalArray(column.getFieldClass());
                column.writeFromComponent(array, index, component);
            }
        }
        
        size++;
    }

    /**
     * Removes an entity from this archetype using swap-and-pop.
     *
     * @param entity the entity to remove
     * @return the components that were associated with the entity
     * @throws IllegalArgumentException if entity doesn't exist
     */
    public Map<Class<?>, Object> removeEntity(Entity entity) {
        Integer index = entityToIndex.get(entity);
        if (index == null) {
            throw new IllegalArgumentException("Entity doesn't exist in archetype: " + entity);
        }
        
        Map<Class<?>, Object> removedComponents = new HashMap<>();
        int lastIndex = size - 1;
        
        // Read removed components and swap with last if needed
        for (Class<?> type : componentTypes) {
            Object component = readComponent(type, index);
            removedComponents.put(type, component);
            
            if (index != lastIndex) {
                // Swap with last element
                List<FieldColumn> columns = componentFieldColumns.get(type);
                for (FieldColumn column : columns) {
                    GlobalArray array = getGlobalArray(column.getFieldClass());
                    Object lastValue = column.readPlain(array, lastIndex);
                    column.writeRelease(array, index, lastValue);
                }
            }
        }
        
        // Update entity tracking
        if (index != lastIndex) {
            Entity lastEntity = entities.get(lastIndex);
            entities.set(index, lastEntity);
            entityToIndex.put(lastEntity, index);
        }
        entities.removeLast();
        entityToIndex.remove(entity);
        size--;
        
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
    public <T> @Nullable T getComponent(Entity entity, Class<T> type) {
        Integer index = entityToIndex.get(entity);
        if (index == null) {
            return null;
        }
        
        if (!componentTypes.contains(type)) {
            return null;
        }
        
        return (T) readComponent(type, index);
    }

    /**
     * Sets a component for an entity.
     *
     * @param entity the entity
     * @param component the component to set
     * @throws IllegalArgumentException if entity doesn't exist or type not in archetype
     */
    public void setComponent(Entity entity, Object component) {
        Integer index = entityToIndex.get(entity);
        if (index == null) {
            throw new IllegalArgumentException("Entity doesn't exist in archetype: " + entity);
        }
        
        Class<?> type = component.getClass();
        List<FieldColumn> columns = componentFieldColumns.get(type);
        if (columns == null) {
            throw new IllegalArgumentException("Component type not in archetype: " + type);
        }
        
        for (FieldColumn column : columns) {
            GlobalArray array = getGlobalArray(column.getFieldClass());
            column.writeFromComponent(array, index, component);
        }
    }

    /**
     * Gets a read-only accessor for a component type.
     * Readers are cached per component type using SoftReferences to avoid repeated allocation
     * while allowing GC to reclaim memory under pressure.
     *
     * @param componentType the component type
     * @param <T> the component type
     * @return a ComponentReader for the specified type
     * @throws IllegalArgumentException if the type is not in this archetype
     */
    @SuppressWarnings("unchecked")
    public <T> ComponentReader<T> getReader(Class<T> componentType) {
        if (!componentTypes.contains(componentType)) {
            throw new IllegalArgumentException("Component type not in archetype: " + componentType);
        }
        
        // Return cached reader if available and not garbage collected
        SoftReference<ComponentReader<?>> ref = cachedReaders.get(componentType);
        ComponentReader<?> cached = (ref != null) ? ref.get() : null;
        if (cached != null) {
            return (ComponentReader<T>) cached;
        }
        
        // Create and cache new reader
        ComponentReader<T> reader = new ComponentReader<>() {
            @Override
            public T read(int entityIndex) {
                if (entityIndex < 0 || entityIndex >= size) {
                    throw new IndexOutOfBoundsException("Index: " + entityIndex + ", Size: " + size);
                }
                return (T) readComponent(componentType, entityIndex);
            }
            
            @Override
            public int size() {
                return size;
            }
        };
        cachedReaders.put(componentType, new SoftReference<>(reader));
        return reader;
    }

    /**
     * Gets a mutable accessor for a component type.
     * Writers are cached per component type using SoftReferences to avoid repeated allocation
     * while allowing GC to reclaim memory under pressure.
     *
     * @param componentType the component type
     * @param <T> the component type
     * @return a ComponentWriter for the specified type
     * @throws IllegalArgumentException if the type is not in this archetype
     */
    @SuppressWarnings("unchecked")
    public <T> ComponentWriter<T> getWriter(Class<T> componentType) {
        if (!componentTypes.contains(componentType)) {
            throw new IllegalArgumentException("Component type not in archetype: " + componentType);
        }
        
        // Return cached writer if available and not garbage collected
        SoftReference<ComponentWriter<?>> ref = cachedWriters.get(componentType);
        ComponentWriter<?> cached = (ref != null) ? ref.get() : null;
        if (cached != null) {
            return (ComponentWriter<T>) cached;
        }
        
        List<FieldColumn> columns = componentFieldColumns.get(componentType);
        
        // Capture reference to archetype for inner class access
        final Archetype archetype = this;
        
        ComponentWriter<T> writer = new ComponentWriter<>() {
            @Override
            public void write(int entityIndex, T component) {
                if (entityIndex < 0 || entityIndex >= size) {
                    throw new IndexOutOfBoundsException("Index: " + entityIndex + ", Size: " + size);
                }
                
                for (FieldColumn column : columns) {
                    GlobalArray array = getGlobalArray(column.getFieldClass());
                    column.writeFromComponent(array, entityIndex, component);
                }
            }
            
            @Override
            public T read(int entityIndex) {
                if (entityIndex < 0 || entityIndex >= size) {
                    throw new IndexOutOfBoundsException("Index: " + entityIndex + ", Size: " + size);
                }
                return (T) archetype.readComponent(componentType, entityIndex);
            }
            
            @Override
            public int size() {
                return size;
            }
        };
        cachedWriters.put(componentType, new SoftReference<>(writer));
        return writer;
    }

    /**
     * Reads a component from the primitive arrays and reconstructs it.
     * Uses thread-local arrays to reduce GC pressure in hot paths.
     */
    private Object readComponent(Class<?> componentType, int entityIndex) {
        List<FieldColumn> columns = componentFieldColumns.get(componentType);
        MethodHandle constructor = componentConstructors.get(componentType);
        
        // Use thread-local array to avoid allocation in hot paths
        int size = columns.size();
        Map<Integer, Object[]> argsCache = THREAD_LOCAL_ARGS.get();
        Object[] args = argsCache.computeIfAbsent(size, k -> new Object[k]);
        
        for (int i = 0; i < size; i++) {
            FieldColumn column = columns.get(i);
            GlobalArray array = getGlobalArray(column.getFieldClass());
            args[column.getFieldIndex()] = column.readPlain(array, entityIndex);
        }
        
        try {
            return constructor.invokeWithArguments(args);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to reconstruct component " + 
                    componentType.getName(), e);
        }
    }

    /**
     * Returns the GlobalArray for a field class.
     */
    private GlobalArray getGlobalArray(Class<?> fieldClass) {
        Class<?> normalizedClass = normalizeFieldClass(fieldClass);
        return globalArrays.get(normalizedClass);
    }

    /**
     * Ensures the archetype has capacity for at least the specified number of entities.
     */
    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= capacity) {
            return;
        }
        
        int newCapacity = (int) Math.max(minCapacity, capacity * GROWTH_FACTOR);
        resizeArrays(newCapacity);
    }

    /**
     * Resizes all global arrays to accommodate more entities.
     */
    private void resizeArrays(int newCapacity) {
        int oldCapacity = capacity;
        
        // Create new arrays for each type
        Map<Class<?>, GlobalArray> newArrays = new HashMap<>();
        for (Map.Entry<Class<?>, GlobalArray> entry : globalArrays.entrySet()) {
            Class<?> fieldClass = entry.getKey();
            GlobalArray oldArray = entry.getValue();
            int numFields = oldArray.length() / oldCapacity;
            GlobalArray newArray = oldArray.createNew(numFields * newCapacity);
            newArrays.put(fieldClass, newArray);
        }
        
        // Track new offsets per type
        Map<Class<?>, Integer> newOffsets = new HashMap<>();
        for (Class<?> fieldClass : globalArrays.keySet()) {
            newOffsets.put(fieldClass, 0);
        }
        
        // Update each field column with new offset and copy data
        for (Class<?> type : componentTypes) {
            List<FieldColumn> columns = componentFieldColumns.get(type);
            for (FieldColumn column : columns) {
                int oldOffset = column.getGlobalOffset();
                Class<?> normalizedClass = normalizeFieldClass(column.getFieldClass());
                
                int newOffset = newOffsets.get(normalizedClass);
                GlobalArray oldArray = globalArrays.get(normalizedClass);
                GlobalArray newArray = newArrays.get(normalizedClass);
                
                oldArray.copyTo(oldOffset, newArray, newOffset, size);
                
                column.setGlobalOffset(newOffset);
                newOffsets.put(normalizedClass, newOffset + newCapacity);
            }
        }
        
        // Replace arrays
        globalArrays.clear();
        globalArrays.putAll(newArrays);
        
        capacity = newCapacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Archetype that = (Archetype) o;
        return Objects.equals(componentTypes, that.componentTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentTypes);
    }

    @Override
    public String toString() {
        return "Archetype{" +
                "componentTypes=" + componentTypes +
                ", entityCount=" + size +
                ", capacity=" + capacity +
                '}';
    }
}
