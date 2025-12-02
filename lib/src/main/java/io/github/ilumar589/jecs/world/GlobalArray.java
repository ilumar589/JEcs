package io.github.ilumar589.jecs.world;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A sealed interface representing typed arrays for component field storage.
 * Each implementation wraps a specific primitive array type and provides
 * type-safe access with VarHandle semantics.
 * 
 * <h2>Benefits over PrimitiveType enum</h2>
 * <ul>
 *   <li><b>Single source of truth:</b> The sealed interface IS the type system</li>
 *   <li><b>No enum → array mapping:</b> Direct access to typed arrays</li>
 *   <li><b>Type-safe operations:</b> Each record knows its array type, compiler enforces correctness</li>
 *   <li><b>Pattern matching:</b> Exhaustiveness checking ensures all types are handled</li>
 *   <li><b>Better encapsulation:</b> VarHandle logic lives with the array wrapper</li>
 * </ul>
 * 
 * <h2>VarHandle Usage</h2>
 * VarHandles provide type-safe, high-performance access to array elements:
 * <ul>
 *   <li>{@code getPlain} - Read without memory barriers (maximum performance)</li>
 *   <li>{@code setRelease} - Write with release semantics (visibility to other threads)</li>
 * </ul>
 * 
 * <h2>Supported Types</h2>
 * <ul>
 *   <li>int, float, double, long, boolean, byte, short, char</li>
 *   <li>String (treated as a reference type but still decomposed)</li>
 * </ul>
 */
public sealed interface GlobalArray {
    
    /**
     * Returns the length of the underlying array.
     *
     * @return the array length
     */
    int length();
    
    /**
     * Creates a new GlobalArray of the same type with the specified capacity.
     *
     * @param capacity the capacity of the new array
     * @return a new GlobalArray of the same type
     */
    GlobalArray createNew(int capacity);
    
    /**
     * Reads a value from the array at the specified index using plain read semantics.
     * No memory barriers are used, providing maximum read performance.
     *
     * @param index the index to read from
     * @return the value at the specified index (boxed for Object return)
     */
    Object readPlainBoxed(int index);
    
    /**
     * Writes a value to the array at the specified index using release semantics.
     * Ensures proper visibility to other threads.
     *
     * @param index the index to write to
     * @param value the value to write (will be unboxed)
     */
    void writeReleaseBoxed(int index, Object value);
    
    /**
     * Copies elements from this array to another GlobalArray of the same type.
     *
     * @param srcPos the starting position in the source array
     * @param dest the destination GlobalArray (must be same type)
     * @param destPos the starting position in the destination array
     * @param length the number of elements to copy
     */
    void copyTo(int srcPos, GlobalArray dest, int destPos, int length);
    
    /**
     * Creates a GlobalArray for the given field class with the specified capacity.
     *
     * @param fieldClass the class of the field (e.g., int.class, float.class)
     * @param capacity the initial capacity
     * @return a new GlobalArray, or null if the field type is not supported
     */
    static GlobalArray fromClass(Class<?> fieldClass, int capacity) {
        if (fieldClass == int.class || fieldClass == Integer.class) {
            return new IntArray(new int[capacity]);
        } else if (fieldClass == float.class || fieldClass == Float.class) {
            return new FloatArray(new float[capacity]);
        } else if (fieldClass == double.class || fieldClass == Double.class) {
            return new DoubleArray(new double[capacity]);
        } else if (fieldClass == long.class || fieldClass == Long.class) {
            return new LongArray(new long[capacity]);
        } else if (fieldClass == boolean.class || fieldClass == Boolean.class) {
            return new BooleanArray(new boolean[capacity]);
        } else if (fieldClass == byte.class || fieldClass == Byte.class) {
            return new ByteArray(new byte[capacity]);
        } else if (fieldClass == short.class || fieldClass == Short.class) {
            return new ShortArray(new short[capacity]);
        } else if (fieldClass == char.class || fieldClass == Character.class) {
            return new CharArray(new char[capacity]);
        } else if (fieldClass == String.class) {
            return new StringArray(new String[capacity]);
        }
        return null;
    }
    
    /**
     * Checks if a field class is supported for GlobalArray storage.
     *
     * @param fieldClass the class to check
     * @return true if the field type is supported
     */
    static boolean isSupported(Class<?> fieldClass) {
        return fieldClass == int.class || fieldClass == Integer.class ||
               fieldClass == float.class || fieldClass == Float.class ||
               fieldClass == double.class || fieldClass == Double.class ||
               fieldClass == long.class || fieldClass == Long.class ||
               fieldClass == boolean.class || fieldClass == Boolean.class ||
               fieldClass == byte.class || fieldClass == Byte.class ||
               fieldClass == short.class || fieldClass == Short.class ||
               fieldClass == char.class || fieldClass == Character.class ||
               fieldClass == String.class;
    }
    
    /**
     * Integer array implementation with int[] storage.
     */
    record IntArray(int[] array) implements GlobalArray {
        private static final VarHandle HANDLE = MethodHandles.arrayElementVarHandle(int[].class);
        
        @Override
        public int length() {
            return array.length;
        }
        
        @Override
        public GlobalArray createNew(int capacity) {
            return new IntArray(new int[capacity]);
        }
        
        /**
         * Reads a value using plain read semantics.
         *
         * @param index the index to read from
         * @return the int value at the specified index
         */
        public int readPlain(int index) {
            return (int) HANDLE.get(array, index);
        }
        
        @Override
        public Object readPlainBoxed(int index) {
            return readPlain(index);
        }
        
        /**
         * Writes a value using release semantics.
         *
         * @param index the index to write to
         * @param value the int value to write
         */
        public void writeRelease(int index, int value) {
            HANDLE.setRelease(array, index, value);
        }
        
        @Override
        public void writeReleaseBoxed(int index, Object value) {
            writeRelease(index, (int) value);
        }
        
        @Override
        public void copyTo(int srcPos, GlobalArray dest, int destPos, int length) {
            System.arraycopy(array, srcPos, ((IntArray) dest).array, destPos, length);
        }
    }
    
    /**
     * Float array implementation with float[] storage.
     */
    record FloatArray(float[] array) implements GlobalArray {
        private static final VarHandle HANDLE = MethodHandles.arrayElementVarHandle(float[].class);
        
        @Override
        public int length() {
            return array.length;
        }
        
        @Override
        public GlobalArray createNew(int capacity) {
            return new FloatArray(new float[capacity]);
        }
        
        /**
         * Reads a value using plain read semantics.
         *
         * @param index the index to read from
         * @return the float value at the specified index
         */
        public float readPlain(int index) {
            return (float) HANDLE.get(array, index);
        }
        
        @Override
        public Object readPlainBoxed(int index) {
            return readPlain(index);
        }
        
        /**
         * Writes a value using release semantics.
         *
         * @param index the index to write to
         * @param value the float value to write
         */
        public void writeRelease(int index, float value) {
            HANDLE.setRelease(array, index, value);
        }
        
        @Override
        public void writeReleaseBoxed(int index, Object value) {
            writeRelease(index, (float) value);
        }
        
        @Override
        public void copyTo(int srcPos, GlobalArray dest, int destPos, int length) {
            System.arraycopy(array, srcPos, ((FloatArray) dest).array, destPos, length);
        }
    }
    
    /**
     * Double array implementation with double[] storage.
     */
    record DoubleArray(double[] array) implements GlobalArray {
        private static final VarHandle HANDLE = MethodHandles.arrayElementVarHandle(double[].class);
        
        @Override
        public int length() {
            return array.length;
        }
        
        @Override
        public GlobalArray createNew(int capacity) {
            return new DoubleArray(new double[capacity]);
        }
        
        /**
         * Reads a value using plain read semantics.
         *
         * @param index the index to read from
         * @return the double value at the specified index
         */
        public double readPlain(int index) {
            return (double) HANDLE.get(array, index);
        }
        
        @Override
        public Object readPlainBoxed(int index) {
            return readPlain(index);
        }
        
        /**
         * Writes a value using release semantics.
         *
         * @param index the index to write to
         * @param value the double value to write
         */
        public void writeRelease(int index, double value) {
            HANDLE.setRelease(array, index, value);
        }
        
        @Override
        public void writeReleaseBoxed(int index, Object value) {
            writeRelease(index, (double) value);
        }
        
        @Override
        public void copyTo(int srcPos, GlobalArray dest, int destPos, int length) {
            System.arraycopy(array, srcPos, ((DoubleArray) dest).array, destPos, length);
        }
    }
    
    /**
     * Long array implementation with long[] storage.
     */
    record LongArray(long[] array) implements GlobalArray {
        private static final VarHandle HANDLE = MethodHandles.arrayElementVarHandle(long[].class);
        
        @Override
        public int length() {
            return array.length;
        }
        
        @Override
        public GlobalArray createNew(int capacity) {
            return new LongArray(new long[capacity]);
        }
        
        /**
         * Reads a value using plain read semantics.
         *
         * @param index the index to read from
         * @return the long value at the specified index
         */
        public long readPlain(int index) {
            return (long) HANDLE.get(array, index);
        }
        
        @Override
        public Object readPlainBoxed(int index) {
            return readPlain(index);
        }
        
        /**
         * Writes a value using release semantics.
         *
         * @param index the index to write to
         * @param value the long value to write
         */
        public void writeRelease(int index, long value) {
            HANDLE.setRelease(array, index, value);
        }
        
        @Override
        public void writeReleaseBoxed(int index, Object value) {
            writeRelease(index, (long) value);
        }
        
        @Override
        public void copyTo(int srcPos, GlobalArray dest, int destPos, int length) {
            System.arraycopy(array, srcPos, ((LongArray) dest).array, destPos, length);
        }
    }
    
    /**
     * Boolean array implementation with boolean[] storage.
     */
    record BooleanArray(boolean[] array) implements GlobalArray {
        private static final VarHandle HANDLE = MethodHandles.arrayElementVarHandle(boolean[].class);
        
        @Override
        public int length() {
            return array.length;
        }
        
        @Override
        public GlobalArray createNew(int capacity) {
            return new BooleanArray(new boolean[capacity]);
        }
        
        /**
         * Reads a value using plain read semantics.
         *
         * @param index the index to read from
         * @return the boolean value at the specified index
         */
        public boolean readPlain(int index) {
            return (boolean) HANDLE.get(array, index);
        }
        
        @Override
        public Object readPlainBoxed(int index) {
            return readPlain(index);
        }
        
        /**
         * Writes a value using release semantics.
         *
         * @param index the index to write to
         * @param value the boolean value to write
         */
        public void writeRelease(int index, boolean value) {
            HANDLE.setRelease(array, index, value);
        }
        
        @Override
        public void writeReleaseBoxed(int index, Object value) {
            writeRelease(index, (boolean) value);
        }
        
        @Override
        public void copyTo(int srcPos, GlobalArray dest, int destPos, int length) {
            System.arraycopy(array, srcPos, ((BooleanArray) dest).array, destPos, length);
        }
    }
    
    /**
     * Byte array implementation with byte[] storage.
     */
    record ByteArray(byte[] array) implements GlobalArray {
        private static final VarHandle HANDLE = MethodHandles.arrayElementVarHandle(byte[].class);
        
        @Override
        public int length() {
            return array.length;
        }
        
        @Override
        public GlobalArray createNew(int capacity) {
            return new ByteArray(new byte[capacity]);
        }
        
        /**
         * Reads a value using plain read semantics.
         *
         * @param index the index to read from
         * @return the byte value at the specified index
         */
        public byte readPlain(int index) {
            return (byte) HANDLE.get(array, index);
        }
        
        @Override
        public Object readPlainBoxed(int index) {
            return readPlain(index);
        }
        
        /**
         * Writes a value using release semantics.
         *
         * @param index the index to write to
         * @param value the byte value to write
         */
        public void writeRelease(int index, byte value) {
            HANDLE.setRelease(array, index, value);
        }
        
        @Override
        public void writeReleaseBoxed(int index, Object value) {
            writeRelease(index, (byte) value);
        }
        
        @Override
        public void copyTo(int srcPos, GlobalArray dest, int destPos, int length) {
            System.arraycopy(array, srcPos, ((ByteArray) dest).array, destPos, length);
        }
    }
    
    /**
     * Short array implementation with short[] storage.
     */
    record ShortArray(short[] array) implements GlobalArray {
        private static final VarHandle HANDLE = MethodHandles.arrayElementVarHandle(short[].class);
        
        @Override
        public int length() {
            return array.length;
        }
        
        @Override
        public GlobalArray createNew(int capacity) {
            return new ShortArray(new short[capacity]);
        }
        
        /**
         * Reads a value using plain read semantics.
         *
         * @param index the index to read from
         * @return the short value at the specified index
         */
        public short readPlain(int index) {
            return (short) HANDLE.get(array, index);
        }
        
        @Override
        public Object readPlainBoxed(int index) {
            return readPlain(index);
        }
        
        /**
         * Writes a value using release semantics.
         *
         * @param index the index to write to
         * @param value the short value to write
         */
        public void writeRelease(int index, short value) {
            HANDLE.setRelease(array, index, value);
        }
        
        @Override
        public void writeReleaseBoxed(int index, Object value) {
            writeRelease(index, (short) value);
        }
        
        @Override
        public void copyTo(int srcPos, GlobalArray dest, int destPos, int length) {
            System.arraycopy(array, srcPos, ((ShortArray) dest).array, destPos, length);
        }
    }
    
    /**
     * Char array implementation with char[] storage.
     */
    record CharArray(char[] array) implements GlobalArray {
        private static final VarHandle HANDLE = MethodHandles.arrayElementVarHandle(char[].class);
        
        @Override
        public int length() {
            return array.length;
        }
        
        @Override
        public GlobalArray createNew(int capacity) {
            return new CharArray(new char[capacity]);
        }
        
        /**
         * Reads a value using plain read semantics.
         *
         * @param index the index to read from
         * @return the char value at the specified index
         */
        public char readPlain(int index) {
            return (char) HANDLE.get(array, index);
        }
        
        @Override
        public Object readPlainBoxed(int index) {
            return readPlain(index);
        }
        
        /**
         * Writes a value using release semantics.
         *
         * @param index the index to write to
         * @param value the char value to write
         */
        public void writeRelease(int index, char value) {
            HANDLE.setRelease(array, index, value);
        }
        
        @Override
        public void writeReleaseBoxed(int index, Object value) {
            writeRelease(index, (char) value);
        }
        
        @Override
        public void copyTo(int srcPos, GlobalArray dest, int destPos, int length) {
            System.arraycopy(array, srcPos, ((CharArray) dest).array, destPos, length);
        }
    }
    
    /**
     * String array implementation with String[] storage.
     */
    record StringArray(String[] array) implements GlobalArray {
        private static final VarHandle HANDLE = MethodHandles.arrayElementVarHandle(String[].class);
        
        @Override
        public int length() {
            return array.length;
        }
        
        @Override
        public GlobalArray createNew(int capacity) {
            return new StringArray(new String[capacity]);
        }
        
        /**
         * Reads a value using plain read semantics.
         *
         * @param index the index to read from
         * @return the String value at the specified index
         */
        public String readPlain(int index) {
            return (String) HANDLE.get(array, index);
        }
        
        @Override
        public Object readPlainBoxed(int index) {
            return readPlain(index);
        }
        
        /**
         * Writes a value using release semantics.
         *
         * @param index the index to write to
         * @param value the String value to write
         */
        public void writeRelease(int index, String value) {
            HANDLE.setRelease(array, index, value);
        }
        
        @Override
        public void writeReleaseBoxed(int index, Object value) {
            writeRelease(index, (String) value);
        }
        
        @Override
        public void copyTo(int srcPos, GlobalArray dest, int destPos, int length) {
            System.arraycopy(array, srcPos, ((StringArray) dest).array, destPos, length);
        }
    }
}
