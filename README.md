# JEcs - Java Entity Component System

A lightweight, cache-friendly Entity Component System (ECS) framework for Java with automatic parallelization.

## Features

- **Component-focused API** - Query entities by component types without exposing internal details
- **Automatic Parallelization** - Systems automatically run in parallel based on component access patterns
- **Virtual Threads** - Uses Project Loom virtual threads for lightweight concurrent execution
- **Project Valhalla Ready** - Designed for future value type optimizations
- **Type-safe Queries** - Fluent builder API with compile-time type safety

## Requirements

- Java 21+ (for virtual threads)
- Gradle 9.x

## Quick Start

### Creating Entities with Components

```java
EcsWorld world = new EcsWorld();

// Spawn entities with components
world.spawn(new Position(0, 0, 0), new Velocity(1, 0, 0));
world.spawn(new Position(10, 0, 0), new Velocity(-1, 0, 0), new Health(100, 100));

// Batch spawn
world.spawnBatch(1000, 
    () -> new Position(random.nextFloat(), random.nextFloat(), 0),
    () -> new Velocity(1, 0, 0)
);
```

### Querying Components

```java
// Simple query
world.componentQuery()
    .with(Position.class, Velocity.class)
    .forEach(Position.class, Velocity.class, (pos, vel) -> {
        System.out.println("Position: " + pos + ", Velocity: " + vel);
    });

// Query with exclusion
world.componentQuery()
    .with(Health.class)
    .without(Dead.class)
    .forEach(Health.class, health -> {
        // Process only living entities
    });
```

### Systems API

Define systems with their component access patterns:

```java
// Physics system - writes Position, reads Velocity
System physics = new System.Builder("Physics")
    .withMutable(Position.class)
    .withReadOnly(Velocity.class)
    .execute((world, query) -> {
        query.forEachWithAccess(Position.class, Velocity.class, (pos, vel) -> {
            pos.set(new Position(
                pos.get().x() + vel.get().dx(),
                pos.get().y() + vel.get().dy(),
                pos.get().z() + vel.get().dz()
            ));
        });
    })
    .build();

// Health regeneration system - writes Health
System healthRegen = new System.Builder("HealthRegen")
    .withMutable(Health.class)
    .without(Dead.class)
    .execute((world, query) -> {
        query.forEachMutable(Health.class, h -> {
            if (h.get().current() < h.get().max()) {
                h.set(new Health(h.get().current() + 1, h.get().max()));
            }
        });
    })
    .build();

// Render system - reads Position
System render = new System.Builder("Render")
    .withReadOnly(Position.class)
    .execute((world, query) -> {
        query.forEach(Position.class, pos -> {
            // Render entity at position
        });
    })
    .build();
```

### Automatic Parallelization

The scheduler automatically determines which systems can run in parallel:

```java
SystemScheduler scheduler = new SystemScheduler.Builder()
    .addSystem(physics)       // Writes Position, reads Velocity
    .addSystem(healthRegen)   // Writes Health (no conflict with physics)
    .addSystem(render)        // Reads Position (must wait for physics)
    .build();

// Execute all systems - automatically parallelized!
// physics and healthRegen run in parallel (no conflicts)
// render runs after physics (reads what physics writes)
scheduler.execute(world);

// Don't forget to shutdown when done
scheduler.shutdown();
```

### Explicit Ordering (Optional)

Use `runInSequence()` when you need ordering beyond automatic conflict detection:

```java
SystemScheduler scheduler = new SystemScheduler.Builder()
    .addSystems(input, physics, render)
    .runInSequence(input, physics)  // Force input before physics
    .build();
```

## Building

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test
```

## Running Performance Benchmarks

JEcs includes JMH (Java Microbenchmark Harness) benchmarks to measure performance.

### Run Benchmarks and Generate HTML Report

```bash
# Run all benchmarks and generate results
./gradlew jmh

# Generate HTML report with charts
./gradlew jmhReport

# Open the report
open lib/build/reports/jmh/report.html   # macOS
xdg-open lib/build/reports/jmh/report.html  # Linux
start lib/build/reports/jmh/report.html  # Windows
```

### Run Specific Benchmarks

```bash
# Run only System benchmarks
./gradlew jmh --args="-rf json -rff build/reports/jmh/results.json SystemBenchmark"

# Run with more iterations for accurate results
./gradlew jmh --args="-rf json -rff build/reports/jmh/results.json -wi 5 -i 10 -f 3"
```

### Benchmark Options

| Option | Description | Default |
|--------|-------------|---------|
| `-wi`  | Warmup iterations | 2 |
| `-i`   | Measurement iterations | 3 |
| `-f`   | Forks (separate JVM runs) | 1 |
| `-tu`  | Time unit (ns, us, ms, s) | us |

### What's Benchmarked

1. **System Execution** - Sequential vs parallel execution with varying entity counts
2. **Component Access** - Conflict detection and access pattern building
3. **Scheduler Building** - Stage computation with different system configurations

## Project Structure

```
lib/src/main/java/io/github/ilumar589/jecs/
├── component/          # Example components (Position, Velocity, Health, etc.)
├── entity/             # Entity class
├── query/              # Component query API
├── system/             # Systems API (System, SystemScheduler, ComponentAccess)
└── world/              # EcsWorld and Archetype storage

lib/src/test/java/      # Unit tests
lib/src/jmh/java/       # JMH performance benchmarks
```

## License

This project is open source. See LICENSE file for details.
