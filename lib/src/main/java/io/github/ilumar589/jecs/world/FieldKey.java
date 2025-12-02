package io.github.ilumar589.jecs.world;

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
 * FieldKey keyA = new FieldKey(ComponentA.class, "health", int.class);
 * FieldKey keyB = new FieldKey(ComponentB.class, "health", int.class);
 * // keyA.equals(keyB) returns false
 * }</pre>
 *
 * @param componentType the class of the component containing the field
 * @param fieldName the name of the field
 * @param fieldClass the class of the field (e.g., int.class, float.class)
 */
public record FieldKey(Class<?> componentType, String fieldName, Class<?> fieldClass) {
    
    /**
     * Creates a new FieldKey.
     *
     * @param componentType the class of the component containing the field
     * @param fieldName the name of the field
     * @param fieldClass the class of the field
     * @throws IllegalArgumentException if any parameter is null or if fieldClass is not supported
     */
    public FieldKey {
        if (componentType == null) {
            throw new IllegalArgumentException("componentType must not be null");
        }
        if (fieldName == null || fieldName.isEmpty()) {
            throw new IllegalArgumentException("fieldName must not be null or empty");
        }
        if (fieldClass == null) {
            throw new IllegalArgumentException("fieldClass must not be null");
        }
        if (!GlobalArray.isSupported(fieldClass)) {
            throw new IllegalArgumentException("Unsupported field type: " + fieldClass.getName());
        }
    }
    
    @Override
    public String toString() {
        return componentType.getSimpleName() + "." + fieldName + "(" + fieldClass.getSimpleName() + ")";
    }
}
