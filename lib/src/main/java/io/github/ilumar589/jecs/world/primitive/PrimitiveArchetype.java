package io.github.ilumar589.jecs.world.primitive;

import io.github.ilumar589.jecs.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.RecordComponent;
import java.util.*;

/**
 * An archetype implementation that decomposes record components into their primitive
 * fields for improved cache locality, SIMD vectorization, and reduced memory overhead.
 * 
 * <h2>Storage Model</h2>
 * Unlike the standard {@code Archetype} which stores component objects, PrimitiveArchetype
 * stores each primitive field in its own typed array:
 * <pre>
 * Standard Archetype:
 *   Position[] -> [Position(1,2,3), Position(4,5,6), ...]  // Object references
 * 
 * PrimitiveArchetype:
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
public final class PrimitiveArchetype {
    /**
     * Default initial capacity for entity storage.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    
    /**
     * Growth factor when resizing arrays.
     */
    private static final double GROWTH_FACTOR = 1.5;

    // Global typed arrays - ONE per primitive type
    private int[] globalInts;
    private float[] globalFloats;
    private double[] globalDoubles;
    private long[] globalLongs;
    private boolean[] globalBooleans;
    private byte[] globalBytes;
    private short[] globalShorts;
    private char[] globalChars;
    private String[] globalStrings;
    
    // Tracks allocation within each global array
    private int intOffset = 0;
    private int floatOffset = 0;
    private int doubleOffset = 0;
    private int longOffset = 0;
    private int booleanOffset = 0;
    private int byteOffset = 0;
    private int shortOffset = 0;
    private int charOffset = 0;
    private int stringOffset = 0;

    private final Set<Class<?>> componentTypes;
    private final Map<FieldKey, FieldColumn> fieldColumns;
    private final Map<Class<?>, List<FieldColumn>> componentFieldColumns;
    private final Map<Class<?>, MethodHandle> componentConstructors;
    
    private final List<Entity> entities;
    private final Map<Entity, Integer> entityToIndex;
    
    private int capacity;
    private int size;

    /**
     * Creates a new PrimitiveArchetype for the given component types.
     * All component types must be records with supported primitive fields.
     *
     * @param componentTypes the set of component types
     * @throws IllegalArgumentException if any type is not a record or has unsupported fields
     */
    public PrimitiveArchetype(Set<Class<?>> componentTypes) {
        this(componentTypes, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Creates a new PrimitiveArchetype with a specified initial capacity.
     *
     * @param componentTypes the set of component types
     * @param initialCapacity the initial storage capacity
     * @throws IllegalArgumentException if any type is not a record or has unsupported fields
     */
    public PrimitiveArchetype(Set<Class<?>> componentTypes, int initialCapacity) {
        this.componentTypes = Set.copyOf(componentTypes);
        this.capacity = Math.max(1, initialCapacity);
        this.size = 0;
        
        this.fieldColumns = new HashMap<>();
        this.componentFieldColumns = new HashMap<>();
        this.componentConstructors = new HashMap<>();
        this.entities = new ArrayList<>(initialCapacity);
        this.entityToIndex = new HashMap<>();
        
        // Count fields per type to allocate arrays
        int totalInts = 0, totalFloats = 0, totalDoubles = 0, totalLongs = 0;
        int totalBooleans = 0, totalBytes = 0, totalShorts = 0, totalChars = 0;
        int totalStrings = 0;
        
        for (Class<?> type : componentTypes) {
            if (!type.isRecord()) {
                throw new IllegalArgumentException("Component type must be a record: " + type.getName());
            }
            
            RecordComponent[] components = type.getRecordComponents();
            for (RecordComponent rc : components) {
                PrimitiveType pt = PrimitiveType.fromClass(rc.getType());
                if (pt == null) {
                    throw new IllegalArgumentException(
                        "Unsupported field type: " + rc.getType().getName() + 
                        " for field " + rc.getName() + " in " + type.getName());
                }
                
                switch (pt) {
                    case INT -> totalInts++;
                    case FLOAT -> totalFloats++;
                    case DOUBLE -> totalDoubles++;
                    case LONG -> totalLongs++;
                    case BOOLEAN -> totalBooleans++;
                    case BYTE -> totalBytes++;
                    case SHORT -> totalShorts++;
                    case CHAR -> totalChars++;
                    case STRING -> totalStrings++;
                }
            }
        }
        
        // Allocate global arrays
        globalInts = totalInts > 0 ? new int[totalInts * capacity] : new int[0];
        globalFloats = totalFloats > 0 ? new float[totalFloats * capacity] : new float[0];
        globalDoubles = totalDoubles > 0 ? new double[totalDoubles * capacity] : new double[0];
        globalLongs = totalLongs > 0 ? new long[totalLongs * capacity] : new long[0];
        globalBooleans = totalBooleans > 0 ? new boolean[totalBooleans * capacity] : new boolean[0];
        globalBytes = totalBytes > 0 ? new byte[totalBytes * capacity] : new byte[0];
        globalShorts = totalShorts > 0 ? new short[totalShorts * capacity] : new short[0];
        globalChars = totalChars > 0 ? new char[totalChars * capacity] : new char[0];
        globalStrings = totalStrings > 0 ? new String[totalStrings * capacity] : new String[0];
        
        // Create field columns for each component type
        for (Class<?> type : componentTypes) {
            List<FieldColumn> columns = new ArrayList<>();
            RecordComponent[] components = type.getRecordComponents();
            
            for (int i = 0; i < components.length; i++) {
                RecordComponent rc = components[i];
                PrimitiveType pt = PrimitiveType.fromClass(rc.getType());
                
                int offset = allocateFieldOffset(pt);
                FieldColumn column = new FieldColumn(type, rc, i, offset);
                
                fieldColumns.put(column.getFieldKey(), column);
                columns.add(column);
            }
            
            componentFieldColumns.put(type, columns);
            componentConstructors.put(type, createConstructorHandle(type, components));
        }
    }

    /**
     * Allocates space in the appropriate global array for a field.
     *
     * @param type the primitive type
     * @return the starting offset for this field's data
     */
    private int allocateFieldOffset(PrimitiveType type) {
        return switch (type) {
            case INT -> {
                int offset = intOffset;
                intOffset += capacity;
                yield offset;
            }
            case FLOAT -> {
                int offset = floatOffset;
                floatOffset += capacity;
                yield offset;
            }
            case DOUBLE -> {
                int offset = doubleOffset;
                doubleOffset += capacity;
                yield offset;
            }
            case LONG -> {
                int offset = longOffset;
                longOffset += capacity;
                yield offset;
            }
            case BOOLEAN -> {
                int offset = booleanOffset;
                booleanOffset += capacity;
                yield offset;
            }
            case BYTE -> {
                int offset = byteOffset;
                byteOffset += capacity;
                yield offset;
            }
            case SHORT -> {
                int offset = shortOffset;
                shortOffset += capacity;
                yield offset;
            }
            case CHAR -> {
                int offset = charOffset;
                charOffset += capacity;
                yield offset;
            }
            case STRING -> {
                int offset = stringOffset;
                stringOffset += capacity;
                yield offset;
            }
        };
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
                Object array = getGlobalArray(column.getType());
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
                    Object array = getGlobalArray(column.getType());
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
            Object array = getGlobalArray(column.getType());
            column.writeFromComponent(array, index, component);
        }
    }

    /**
     * Gets a read-only accessor for a component type.
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
        
        return new ComponentReader<>() {
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
    }

    /**
     * Gets a mutable accessor for a component type.
     *
     * @param componentType the component type
     * @param <T> the component type
     * @return a ComponentWriter for the specified type
     * @throws IllegalArgumentException if the type is not in this archetype
     */
    public <T> ComponentWriter<T> getWriter(Class<T> componentType) {
        if (!componentTypes.contains(componentType)) {
            throw new IllegalArgumentException("Component type not in archetype: " + componentType);
        }
        
        List<FieldColumn> columns = componentFieldColumns.get(componentType);
        
        return new ComponentWriter<>() {
            @Override
            public void write(int entityIndex, T component) {
                if (entityIndex < 0 || entityIndex >= size) {
                    throw new IndexOutOfBoundsException("Index: " + entityIndex + ", Size: " + size);
                }
                
                for (FieldColumn column : columns) {
                    Object array = getGlobalArray(column.getType());
                    column.writeFromComponent(array, entityIndex, component);
                }
            }
            
            @Override
            public int size() {
                return size;
            }
        };
    }

    /**
     * Reads a component from the primitive arrays and reconstructs it.
     */
    private Object readComponent(Class<?> componentType, int entityIndex) {
        List<FieldColumn> columns = componentFieldColumns.get(componentType);
        MethodHandle constructor = componentConstructors.get(componentType);
        
        Object[] args = new Object[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            FieldColumn column = columns.get(i);
            Object array = getGlobalArray(column.getType());
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
     * Returns the global array for a primitive type.
     */
    private Object getGlobalArray(PrimitiveType type) {
        return switch (type) {
            case INT -> globalInts;
            case FLOAT -> globalFloats;
            case DOUBLE -> globalDoubles;
            case LONG -> globalLongs;
            case BOOLEAN -> globalBooleans;
            case BYTE -> globalBytes;
            case SHORT -> globalShorts;
            case CHAR -> globalChars;
            case STRING -> globalStrings;
        };
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
        
        // Create new arrays
        int[] newInts = new int[intOffset > 0 ? (intOffset / oldCapacity) * newCapacity : 0];
        float[] newFloats = new float[floatOffset > 0 ? (floatOffset / oldCapacity) * newCapacity : 0];
        double[] newDoubles = new double[doubleOffset > 0 ? (doubleOffset / oldCapacity) * newCapacity : 0];
        long[] newLongs = new long[longOffset > 0 ? (longOffset / oldCapacity) * newCapacity : 0];
        boolean[] newBooleans = new boolean[booleanOffset > 0 ? (booleanOffset / oldCapacity) * newCapacity : 0];
        byte[] newBytes = new byte[byteOffset > 0 ? (byteOffset / oldCapacity) * newCapacity : 0];
        short[] newShorts = new short[shortOffset > 0 ? (shortOffset / oldCapacity) * newCapacity : 0];
        char[] newChars = new char[charOffset > 0 ? (charOffset / oldCapacity) * newCapacity : 0];
        String[] newStrings = new String[stringOffset > 0 ? (stringOffset / oldCapacity) * newCapacity : 0];
        
        // Reset offsets and copy data
        intOffset = 0;
        floatOffset = 0;
        doubleOffset = 0;
        longOffset = 0;
        booleanOffset = 0;
        byteOffset = 0;
        shortOffset = 0;
        charOffset = 0;
        stringOffset = 0;
        
        // Update each field column with new offset and copy data
        for (Class<?> type : componentTypes) {
            List<FieldColumn> columns = componentFieldColumns.get(type);
            for (FieldColumn column : columns) {
                int oldOffset = column.getGlobalOffset();
                int newOffset;
                
                switch (column.getType()) {
                    case INT -> {
                        newOffset = intOffset;
                        System.arraycopy(globalInts, oldOffset, newInts, newOffset, size);
                        intOffset += newCapacity;
                    }
                    case FLOAT -> {
                        newOffset = floatOffset;
                        System.arraycopy(globalFloats, oldOffset, newFloats, newOffset, size);
                        floatOffset += newCapacity;
                    }
                    case DOUBLE -> {
                        newOffset = doubleOffset;
                        System.arraycopy(globalDoubles, oldOffset, newDoubles, newOffset, size);
                        doubleOffset += newCapacity;
                    }
                    case LONG -> {
                        newOffset = longOffset;
                        System.arraycopy(globalLongs, oldOffset, newLongs, newOffset, size);
                        longOffset += newCapacity;
                    }
                    case BOOLEAN -> {
                        newOffset = booleanOffset;
                        System.arraycopy(globalBooleans, oldOffset, newBooleans, newOffset, size);
                        booleanOffset += newCapacity;
                    }
                    case BYTE -> {
                        newOffset = byteOffset;
                        System.arraycopy(globalBytes, oldOffset, newBytes, newOffset, size);
                        byteOffset += newCapacity;
                    }
                    case SHORT -> {
                        newOffset = shortOffset;
                        System.arraycopy(globalShorts, oldOffset, newShorts, newOffset, size);
                        shortOffset += newCapacity;
                    }
                    case CHAR -> {
                        newOffset = charOffset;
                        System.arraycopy(globalChars, oldOffset, newChars, newOffset, size);
                        charOffset += newCapacity;
                    }
                    case STRING -> {
                        newOffset = stringOffset;
                        System.arraycopy(globalStrings, oldOffset, newStrings, newOffset, size);
                        stringOffset += newCapacity;
                    }
                    default -> throw new IllegalStateException("Unknown primitive type: " + column.getType());
                }
                
                column.setGlobalOffset(newOffset);
            }
        }
        
        // Replace arrays
        globalInts = newInts;
        globalFloats = newFloats;
        globalDoubles = newDoubles;
        globalLongs = newLongs;
        globalBooleans = newBooleans;
        globalBytes = newBytes;
        globalShorts = newShorts;
        globalChars = newChars;
        globalStrings = newStrings;
        
        capacity = newCapacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrimitiveArchetype that = (PrimitiveArchetype) o;
        return Objects.equals(componentTypes, that.componentTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentTypes);
    }

    @Override
    public String toString() {
        return "PrimitiveArchetype{" +
                "componentTypes=" + componentTypes +
                ", entityCount=" + size +
                ", capacity=" + capacity +
                '}';
    }
}
