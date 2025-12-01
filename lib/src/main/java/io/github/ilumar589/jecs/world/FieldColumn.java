package io.github.ilumar589.jecs.world;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.RecordComponent;

/**
 * Represents a column of primitive values for a single field across all entities.
 * Each FieldColumn tracks:
 * <ul>
 *   <li>The primitive type of the field</li>
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
    private final PrimitiveType type;
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
        
        PrimitiveType primitiveType = PrimitiveType.fromClass(fieldType);
        if (primitiveType == null) {
            throw new IllegalArgumentException(
                "Unsupported field type: " + fieldType.getName() + 
                " for field " + fieldName + " in " + componentType.getName());
        }
        
        this.fieldKey = new FieldKey(componentType, fieldName, primitiveType);
        this.type = primitiveType;
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
     * Returns the primitive type of this field.
     *
     * @return the primitive type
     */
    public PrimitiveType getType() {
        return type;
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
     * Reads a value from the global array using plain read semantics.
     * No memory barriers are used, providing maximum read performance.
     *
     * @param globalArray the global array for this primitive type
     * @param entityIndex the index of the entity
     * @return the value at the specified entity index
     */
    public Object readPlain(Object globalArray, int entityIndex) {
        return type.readPlain(globalArray, globalOffset + entityIndex);
    }

    /**
     * Writes a value to the global array using release semantics.
     * Ensures proper visibility to other threads.
     *
     * @param globalArray the global array for this primitive type
     * @param entityIndex the index of the entity
     * @param value the value to write
     */
    public void writeRelease(Object globalArray, int entityIndex, Object value) {
        type.writeRelease(globalArray, globalOffset + entityIndex, value);
    }

    /**
     * Writes a component's field value to the global array.
     *
     * @param globalArray the global array for this primitive type
     * @param entityIndex the index of the entity
     * @param component the component to extract the value from
     */
    public void writeFromComponent(Object globalArray, int entityIndex, Object component) {
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
