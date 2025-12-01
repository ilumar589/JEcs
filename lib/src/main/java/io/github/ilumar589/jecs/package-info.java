/**
 * JEcs - A lightweight, cache-friendly Entity Component System (ECS) framework.
 * 
 * <p>This package serves as the root for the JEcs framework, which provides:
 * <ul>
 *   <li>Primitive decomposition for cache-efficient component storage</li>
 *   <li>VarHandle-based access for high-performance reads and writes</li>
 *   <li>Full Project Valhalla value type readiness</li>
 * </ul>
 * 
 * <h2>Null Safety</h2>
 * This package and all subpackages use JSpecify annotations for null safety.
 * All types are non-null by default unless annotated with {@code @Nullable}.
 */
@NullMarked
package io.github.ilumar589.jecs;

import org.jspecify.annotations.NullMarked;
