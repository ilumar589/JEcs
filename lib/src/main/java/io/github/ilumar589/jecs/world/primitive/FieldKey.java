package io.github.ilumar589.jecs.world.primitive;

/**
 * A unique key identifying a field within a component type.
 * This ensures that fields with the same name in different component types
 * are stored separately.
 * 
 * <h2>Example</h2>
 * <pre>{@code
 * record ComponentA(int health) {}
 * record ComponentB(int health) {}
 * 
 * // These create different FieldKeys:
 * FieldKey keyA = new FieldKey(ComponentA.class, "health", PrimitiveType.INT);
 * FieldKey keyB = new FieldKey(ComponentB.class, "health", PrimitiveType.INT);
 * // keyA.equals(keyB) returns false
 * }</pre>
 *
 * @param componentType the class of the component containing the field
 * @param fieldName the name of the field
 * @param primitiveType the primitive type of the field
 */
public record FieldKey(Class<?> componentType, String fieldName, PrimitiveType primitiveType) {
    
    /**
     * Creates a new FieldKey.
     *
     * @param componentType the class of the component containing the field
     * @param fieldName the name of the field
     * @param primitiveType the primitive type of the field
     * @throws IllegalArgumentException if any parameter is null
     */
    public FieldKey {
        if (componentType == null) {
            throw new IllegalArgumentException("componentType must not be null");
        }
        if (fieldName == null || fieldName.isEmpty()) {
            throw new IllegalArgumentException("fieldName must not be null or empty");
        }
        if (primitiveType == null) {
            throw new IllegalArgumentException("primitiveType must not be null");
        }
    }
    
    @Override
    public String toString() {
        return componentType.getSimpleName() + "." + fieldName + "(" + primitiveType + ")";
    }
}
