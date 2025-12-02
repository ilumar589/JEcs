package io.github.ilumar589.jecs.world;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.RecordComponent;

/**
 * Represents a column of primitive values for a single field across all entities.
 * Each FieldColumn tracks:
 * <ul>
 *   <li>The field class (e.g., int.class, float.class)</li>
 *   <li>The offset in the global array where this field's data starts</li>
 *   <li>A MethodHandle to extract the field value from component instances</li>
 * </ul>
 * 
 * <h2>Memory Layout</h2>
 * For an archetype with N entities and a component with field "x":
 * <pre>
 * Global float array: [..., x[0], x[1], x[2], ..., x[N-1], ...]
 *                         ^--- globalOffset
 * </pre>
 * 
 * <h2>Thread Safety</h2>
 * Uses VarHandle operations with appropriate memory semantics:
 * <ul>
 *   <li>{@link #readPlain} uses getPlain for maximum read performance</li>
 *   <li>{@link #writeRelease} uses setRelease for safe writes</li>
 * </ul>
 */
public final class FieldColumn {
    private final FieldKey fieldKey;
    private final Class<?> fieldClass;
    private final int fieldIndex;
    private final MethodHandle fieldGetter;
    private int globalOffset;

    /**
     * Creates a new FieldColumn for a record component.
     *
     * @param componentType the component class containing the field
     * @param recordComponent the record component representing the field
     * @param fieldIndex the index of this field in the record's component order
     * @param globalOffset the starting offset in the global array
     * @throws IllegalArgumentException if the field type is not supported
     * @throws RuntimeException if the method handle cannot be created
     */
    public FieldColumn(Class<?> componentType, RecordComponent recordComponent, 
                       int fieldIndex, int globalOffset) {
        String fieldName = recordComponent.getName();
        Class<?> fieldType = recordComponent.getType();
        
        if (!GlobalArray.isSupported(fieldType)) {
            throw new IllegalArgumentException(
                "Unsupported field type: " + fieldType.getName() + 
                " for field " + fieldName + " in " + componentType.getName());
        }
        
        this.fieldKey = new FieldKey(componentType, fieldName, fieldType);
        this.fieldClass = fieldType;
        this.fieldIndex = fieldIndex;
        this.globalOffset = globalOffset;
        
        try {
            this.fieldGetter = MethodHandles.lookup()
                    .unreflect(recordComponent.getAccessor());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field " + fieldName + 
                    " in " + componentType.getName(), e);
        }
    }

    /**
     * Returns the unique key for this field.
     *
     * @return the field key
     */
    public FieldKey getFieldKey() {
        return fieldKey;
    }

    /**
     * Returns the field class of this field (e.g., int.class, float.class).
     *
     * @return the field class
     */
    public Class<?> getFieldClass() {
        return fieldClass;
    }

    /**
     * Returns the index of this field in the record's component order.
     *
     * @return the field index
     */
    public int getFieldIndex() {
        return fieldIndex;
    }

    /**
     * Returns the starting offset in the global array.
     *
     * @return the global offset
     */
    public int getGlobalOffset() {
        return globalOffset;
    }

    /**
     * Updates the global offset. Used when reallocating arrays.
     *
     * @param newOffset the new offset
     */
    void setGlobalOffset(int newOffset) {
        this.globalOffset = newOffset;
    }

    /**
     * Extracts the field value from a component instance.
     *
     * @param component the component instance
     * @return the field value
     * @throws RuntimeException if extraction fails
     */
    public Object extractValue(Object component) {
        try {
            return fieldGetter.invoke(component);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to extract field " + 
                    fieldKey.fieldName() + " from " + component.getClass().getName(), e);
        }
    }

    /**
     * Reads a value from the GlobalArray using plain read semantics.
     * No memory barriers are used, providing maximum read performance.
     * 
     * <h2>Performance Note</h2>
     * This method uses pattern matching to avoid boxing overhead by directly 
     * calling typed read methods on the GlobalArray implementation.
     * 
     * <h2>Exhaustiveness</h2>
     * The switch is exhaustive because {@link GlobalArray} is a sealed interface.
     * The compiler ensures all permitted implementations are handled.
     *
     * @param globalArray the GlobalArray for this field type
     * @param entityIndex the index of the entity
     * @return the value at the specified entity index
     */
    public Object readPlain(GlobalArray globalArray, int entityIndex) {
        int index = globalOffset + entityIndex;
        // Use pattern matching to avoid boxing in hot paths
        // Switch is exhaustive - GlobalArray is a sealed interface
        return switch (globalArray) {
            case GlobalArray.IntArray arr -> arr.readPlain(index);
            case GlobalArray.FloatArray arr -> arr.readPlain(index);
            case GlobalArray.DoubleArray arr -> arr.readPlain(index);
            case GlobalArray.LongArray arr -> arr.readPlain(index);
            case GlobalArray.BooleanArray arr -> arr.readPlain(index);
            case GlobalArray.ByteArray arr -> arr.readPlain(index);
            case GlobalArray.ShortArray arr -> arr.readPlain(index);
            case GlobalArray.CharArray arr -> arr.readPlain(index);
            case GlobalArray.StringArray arr -> arr.readPlain(index);
        };
    }

    /**
     * Writes a value to the GlobalArray using release semantics.
     * Ensures proper visibility to other threads.
     * 
     * <h2>Performance Note</h2>
     * This method uses pattern matching to avoid unboxing overhead by directly 
     * calling typed write methods on the GlobalArray implementation.
     * 
     * <h2>Exhaustiveness</h2>
     * The switch is exhaustive because {@link GlobalArray} is a sealed interface.
     * The compiler ensures all permitted implementations are handled.
     * 
     * <h2>Type Safety</h2>
     * Callers must ensure the value type matches the GlobalArray type.
     * A ClassCastException will be thrown if types don't match.
     *
     * @param globalArray the GlobalArray for this field type
     * @param entityIndex the index of the entity
     * @param value the value to write (must match the GlobalArray's element type)
     * @throws ClassCastException if the value type doesn't match the array type
     */
    public void writeRelease(GlobalArray globalArray, int entityIndex, Object value) {
        int index = globalOffset + entityIndex;
        // Use pattern matching to avoid boxing/unboxing in hot paths
        // Switch is exhaustive - GlobalArray is a sealed interface
        switch (globalArray) {
            case GlobalArray.IntArray arr -> arr.writeRelease(index, (int) value);
            case GlobalArray.FloatArray arr -> arr.writeRelease(index, (float) value);
            case GlobalArray.DoubleArray arr -> arr.writeRelease(index, (double) value);
            case GlobalArray.LongArray arr -> arr.writeRelease(index, (long) value);
            case GlobalArray.BooleanArray arr -> arr.writeRelease(index, (boolean) value);
            case GlobalArray.ByteArray arr -> arr.writeRelease(index, (byte) value);
            case GlobalArray.ShortArray arr -> arr.writeRelease(index, (short) value);
            case GlobalArray.CharArray arr -> arr.writeRelease(index, (char) value);
            case GlobalArray.StringArray arr -> arr.writeRelease(index, (String) value);
        }
    }

    /**
     * Writes a component's field value to the GlobalArray.
     *
     * @param globalArray the GlobalArray for this field type
     * @param entityIndex the index of the entity
     * @param component the component to extract the value from
     */
    public void writeFromComponent(GlobalArray globalArray, int entityIndex, Object component) {
        Object value = extractValue(component);
        writeRelease(globalArray, entityIndex, value);
    }

    @Override
    public String toString() {
        return "FieldColumn{" +
                "key=" + fieldKey +
                ", offset=" + globalOffset +
                '}';
    }
}
