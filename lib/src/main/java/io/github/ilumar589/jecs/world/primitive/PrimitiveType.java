package io.github.ilumar589.jecs.world.primitive;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Represents the primitive types supported for component field decomposition.
 * Each type has an associated VarHandle for efficient array element access.
 * 
 * <h2>VarHandle Usage</h2>
 * VarHandles provide type-safe, high-performance access to array elements:
 * <ul>
 *   <li>{@code getPlain} - Read without memory barriers (maximum performance)</li>
 *   <li>{@code setRelease} - Write with release semantics (visibility to other threads)</li>
 * </ul>
 * 
 * <h2>Type Safety</h2>
 * Each primitive type has its own separate array, eliminating type confusion
 * and enabling JIT optimizations like auto-vectorization.
 */
public enum PrimitiveType {
    /**
     * Integer primitive type with int[] storage.
     */
    INT(int.class, int[].class, MethodHandles.arrayElementVarHandle(int[].class)),
    
    /**
     * Float primitive type with float[] storage.
     */
    FLOAT(float.class, float[].class, MethodHandles.arrayElementVarHandle(float[].class)),
    
    /**
     * Double primitive type with double[] storage.
     */
    DOUBLE(double.class, double[].class, MethodHandles.arrayElementVarHandle(double[].class)),
    
    /**
     * Long primitive type with long[] storage.
     */
    LONG(long.class, long[].class, MethodHandles.arrayElementVarHandle(long[].class)),
    
    /**
     * Boolean primitive type with boolean[] storage.
     */
    BOOLEAN(boolean.class, boolean[].class, MethodHandles.arrayElementVarHandle(boolean[].class)),
    
    /**
     * Byte primitive type with byte[] storage.
     */
    BYTE(byte.class, byte[].class, MethodHandles.arrayElementVarHandle(byte[].class)),
    
    /**
     * Short primitive type with short[] storage.
     */
    SHORT(short.class, short[].class, MethodHandles.arrayElementVarHandle(short[].class)),
    
    /**
     * Char primitive type with char[] storage.
     */
    CHAR(char.class, char[].class, MethodHandles.arrayElementVarHandle(char[].class)),
    
    /**
     * String reference type with String[] storage.
     * Included for common string fields in components.
     */
    STRING(String.class, String[].class, MethodHandles.arrayElementVarHandle(String[].class));

    private final Class<?> primitiveClass;
    private final Class<?> arrayClass;
    private final VarHandle arrayHandle;

    PrimitiveType(Class<?> primitiveClass, Class<?> arrayClass, VarHandle arrayHandle) {
        this.primitiveClass = primitiveClass;
        this.arrayClass = arrayClass;
        this.arrayHandle = arrayHandle;
    }

    /**
     * Returns the primitive class (e.g., int.class, float.class).
     *
     * @return the primitive class type
     */
    public Class<?> getPrimitiveClass() {
        return primitiveClass;
    }

    /**
     * Returns the array class (e.g., int[].class, float[].class).
     *
     * @return the array class type
     */
    public Class<?> getArrayClass() {
        return arrayClass;
    }

    /**
     * Returns the VarHandle for array element access.
     *
     * @return the VarHandle for this primitive type's array
     */
    public VarHandle getArrayHandle() {
        return arrayHandle;
    }

    /**
     * Reads a value from the array at the specified index using plain read semantics.
     * No memory barriers are used, providing maximum read performance.
     *
     * @param array the array to read from (must match this type's array class)
     * @param index the index to read from
     * @return the value at the specified index
     */
    public Object readPlain(Object array, int index) {
        return arrayHandle.get(array, index);
    }

    /**
     * Writes a value to the array at the specified index using release semantics.
     * Ensures proper visibility to other threads.
     *
     * @param array the array to write to (must match this type's array class)
     * @param index the index to write to
     * @param value the value to write
     */
    public void writeRelease(Object array, int index, Object value) {
        arrayHandle.setRelease(array, index, value);
    }

    /**
     * Finds the PrimitiveType for a given field class.
     *
     * @param fieldClass the class of the field
     * @return the corresponding PrimitiveType, or null if not supported
     */
    public static PrimitiveType fromClass(Class<?> fieldClass) {
        if (fieldClass == int.class || fieldClass == Integer.class) {
            return INT;
        } else if (fieldClass == float.class || fieldClass == Float.class) {
            return FLOAT;
        } else if (fieldClass == double.class || fieldClass == Double.class) {
            return DOUBLE;
        } else if (fieldClass == long.class || fieldClass == Long.class) {
            return LONG;
        } else if (fieldClass == boolean.class || fieldClass == Boolean.class) {
            return BOOLEAN;
        } else if (fieldClass == byte.class || fieldClass == Byte.class) {
            return BYTE;
        } else if (fieldClass == short.class || fieldClass == Short.class) {
            return SHORT;
        } else if (fieldClass == char.class || fieldClass == Character.class) {
            return CHAR;
        } else if (fieldClass == String.class) {
            return STRING;
        }
        return null;
    }

    /**
     * Creates a new array of this primitive type with the specified capacity.
     *
     * @param capacity the capacity of the array to create
     * @return a new array of the appropriate type
     */
    public Object createArray(int capacity) {
        return switch (this) {
            case INT -> new int[capacity];
            case FLOAT -> new float[capacity];
            case DOUBLE -> new double[capacity];
            case LONG -> new long[capacity];
            case BOOLEAN -> new boolean[capacity];
            case BYTE -> new byte[capacity];
            case SHORT -> new short[capacity];
            case CHAR -> new char[capacity];
            case STRING -> new String[capacity];
        };
    }

    /**
     * Returns the length of the given array.
     *
     * @param array the array
     * @return the length of the array
     */
    public int arrayLength(Object array) {
        return switch (this) {
            case INT -> ((int[]) array).length;
            case FLOAT -> ((float[]) array).length;
            case DOUBLE -> ((double[]) array).length;
            case LONG -> ((long[]) array).length;
            case BOOLEAN -> ((boolean[]) array).length;
            case BYTE -> ((byte[]) array).length;
            case SHORT -> ((short[]) array).length;
            case CHAR -> ((char[]) array).length;
            case STRING -> ((String[]) array).length;
        };
    }

    /**
     * Copies elements from one array to another.
     *
     * @param src the source array
     * @param srcPos the starting position in the source array
     * @param dest the destination array
     * @param destPos the starting position in the destination array
     * @param length the number of elements to copy
     */
    public void arrayCopy(Object src, int srcPos, Object dest, int destPos, int length) {
        switch (this) {
            case INT -> System.arraycopy((int[]) src, srcPos, (int[]) dest, destPos, length);
            case FLOAT -> System.arraycopy((float[]) src, srcPos, (float[]) dest, destPos, length);
            case DOUBLE -> System.arraycopy((double[]) src, srcPos, (double[]) dest, destPos, length);
            case LONG -> System.arraycopy((long[]) src, srcPos, (long[]) dest, destPos, length);
            case BOOLEAN -> System.arraycopy((boolean[]) src, srcPos, (boolean[]) dest, destPos, length);
            case BYTE -> System.arraycopy((byte[]) src, srcPos, (byte[]) dest, destPos, length);
            case SHORT -> System.arraycopy((short[]) src, srcPos, (short[]) dest, destPos, length);
            case CHAR -> System.arraycopy((char[]) src, srcPos, (char[]) dest, destPos, length);
            case STRING -> System.arraycopy((String[]) src, srcPos, (String[]) dest, destPos, length);
        }
    }
}
