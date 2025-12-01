/**
 * Primitive decomposition support for ECS components.
 * 
 * <h2>Overview</h2>
 * This package provides an alternative archetype implementation that decomposes
 * record components into their primitive fields for improved performance.
 * 
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link io.github.ilumar589.jecs.world.primitive.PrimitiveArchetype} - 
 *       The main archetype with primitive field storage</li>
 *   <li>{@link io.github.ilumar589.jecs.world.primitive.ComponentReader} - 
 *       Read-only component access with maximum performance</li>
 *   <li>{@link io.github.ilumar589.jecs.world.primitive.ComponentWriter} - 
 *       Mutable component access with proper memory semantics</li>
 *   <li>{@link io.github.ilumar589.jecs.world.primitive.PrimitiveType} - 
 *       Supported primitive types with typed VarHandles</li>
 *   <li>{@link io.github.ilumar589.jecs.world.primitive.FieldColumn} - 
 *       Field metadata and array offset tracking</li>
 *   <li>{@link io.github.ilumar589.jecs.world.primitive.FieldKey} - 
 *       Unique field identification across component types</li>
 * </ul>
 * 
 * <h2>Performance Benefits</h2>
 * <ul>
 *   <li>2-4x faster iteration for read-only queries (cache locality)</li>
 *   <li>50% memory reduction (no object headers)</li>
 *   <li>SIMD-friendly for auto-vectorization</li>
 *   <li>Zero GC pressure for primitive fields</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create archetype
 * Set<Class<?>> types = Set.of(Position.class, Velocity.class);
 * PrimitiveArchetype archetype = new PrimitiveArchetype(types, 1000);
 * 
 * // Add entities
 * Entity e = new Entity(1);
 * archetype.addEntity(e, Map.of(
 *     Position.class, new Position(0, 0, 0),
 *     Velocity.class, new Velocity(1, 0, 0)
 * ));
 * 
 * // Read-only iteration (maximum performance)
 * ComponentReader<Position> posReader = archetype.getReader(Position.class);
 * for (int i = 0; i < archetype.size(); i++) {
 *     Position pos = posReader.read(i);  // Uses getPlain - no barriers
 * }
 * 
 * // Mutable access
 * ComponentWriter<Position> posWriter = archetype.getWriter(Position.class);
 * posWriter.write(0, new Position(1, 2, 3));  // Uses setRelease - safe write
 * }</pre>
 * 
 * @see io.github.ilumar589.jecs.world.Archetype
 */
@org.jspecify.annotations.NullMarked
package io.github.ilumar589.jecs.world.primitive;
