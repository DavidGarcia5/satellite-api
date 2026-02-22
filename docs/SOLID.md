# Satellite API — SOLID Principles

This document explains how the SOLID principles were applied during a refactoring pass of the Satellite API codebase, and the reasoning behind each change.

---

## What is SOLID?

SOLID is a set of five design principles for writing maintainable, flexible, and testable object-oriented code:

| Letter | Principle | One-Liner |
|--------|-----------|-----------|
| **S** | Single Responsibility | A class should have only one reason to change |
| **O** | Open/Closed | Open for extension, closed for modification |
| **L** | Liskov Substitution | Subtypes must be substitutable for their base types |
| **I** | Interface Segregation | Clients should not depend on interfaces they don't use |
| **D** | Dependency Inversion | Depend on abstractions, not concretions |

---

## Changes Made

### 1. Service Interface — Dependency Inversion Principle (DIP)

**Principle:** *"High-level modules should not depend on low-level modules. Both should depend on abstractions."*

**Before:**
The controller depended directly on the concrete `SatelliteService` class. The controller and service were tightly coupled — if the implementation changed, the controller would be affected.

```java
// Controller was coupled to the concrete class
private final SatelliteService satelliteService;  // concrete class
```

**After:**
`SatelliteService` was extracted into an **interface** (the contract), and the original logic moved to `DefaultSatelliteService` (the implementation).

```java
// Interface — defines the contract
public interface SatelliteService {
    List<Satellite> getAllSatellites();
    Satellite getSatelliteById(Long id);
    // ...
}

// Implementation — contains the business logic
@Service
public class DefaultSatelliteService implements SatelliteService {
    @Override
    public List<Satellite> getAllSatellites() { ... }
}
```

**Why it matters:**
- The controller depends on the abstraction (interface), not the implementation
- Service implementations can be swapped (e.g., a mock for testing, or a different data source) without changing the controller
- Spring automatically wires the `@Service`-annotated implementation into the interface type

**Files changed:**
| File | Change |
|------|--------|
| `SatelliteService.java` | Converted from concrete class to interface |
| `DefaultSatelliteService.java` | New file — concrete implementation with `@Override` methods |
| `SatelliteServiceTest.java` | `@InjectMocks` now targets `DefaultSatelliteService` |

---

### 2. SatelliteResponse DTO — Single Responsibility Principle (SRP)

**Principle:** *"A class should have only one reason to change."*

**Before:**
The `Satellite` JPA entity was returned directly as the API response. This gave it **two responsibilities**: database mapping (JPA annotations) and API response shape (JSON output).

```java
// Entity leaked directly into API responses
public ResponseEntity<Satellite> getSatelliteById(...) {
    return ResponseEntity.ok(satelliteService.getSatelliteById(id));
}
```

The risk: adding a JPA-only field (e.g., `@CreatedDate`, `@Version`) would automatically appear in API responses. Renaming a JSON field could break database mappings.

**After:**
A dedicated `SatelliteResponse` DTO was created with a `fromEntity()` factory method. The entity handles persistence; the DTO handles presentation.

```java
// Response DTO — only concerned with API output shape
public class SatelliteResponse {
    public static SatelliteResponse fromEntity(Satellite satellite) { ... }
}

// Controller returns the DTO, not the entity
public ResponseEntity<SatelliteResponse> getSatelliteById(...) {
    return ResponseEntity.ok(SatelliteResponse.fromEntity(satelliteService.getSatelliteById(id)));
}
```

**Why it matters:**
- The JPA entity can evolve independently of the API contract
- Internal fields (audit timestamps, version columns) won't leak into responses
- The response shape is explicitly documented in one class

**Files changed:**
| File | Change |
|------|--------|
| `SatelliteResponse.java` | New file — output DTO with `fromEntity()` factory method |
| `SatelliteController.java` | Return types changed from `Satellite` to `SatelliteResponse` |

---

### 3. ErrorResponse Record — Single Responsibility Principle (SRP)

**Principle:** *"A class should have only one reason to change."*

**Before:**
Error responses were built using raw `HashMap<String, Object>` inside the exception handler. This had no type safety, no compile-time checks, and duplicated the structure across handler methods.

```java
// No type safety — typos in keys would compile fine
Map<String, Object> body = new HashMap<>();
body.put("timestamp", LocalDateTime.now());
body.put("status", 404);
body.put("error", "Not Found");
body.put("message", ex.getMessage());
```

**After:**
A Java `record` (`ErrorResponse`) defines the error response structure. Factory methods handle the two error variants (single-message and multi-message).

```java
// Typed, immutable, self-documenting
public record ErrorResponse(
    LocalDateTime timestamp, int status, String error,
    String message, List<String> messages
) {
    public static ErrorResponse of(int status, String error, String message) { ... }
    public static ErrorResponse of(int status, String error, List<String> messages) { ... }
}

// Exception handler becomes clean and focused
ErrorResponse error = ErrorResponse.of(404, "Not Found", ex.getMessage());
```

**Why it matters:**
- Compile-time safety — misspelled fields are caught by the compiler
- Immutable — error responses can't be accidentally modified after creation
- Self-documenting — the record is a clear specification of the API error contract
- DRY — both handler methods reuse the same `ErrorResponse` type

**Files changed:**
| File | Change |
|------|--------|
| `ErrorResponse.java` | New file — Java record with factory methods |
| `GlobalExceptionHandler.java` | Replaced `HashMap` construction with `ErrorResponse.of()` |

---

### 4. SLF4J Logging — Operational Best Practice

**Not a SOLID principle per se**, but a professional standard that complements SRP — it keeps operational concerns (observability) cleanly separated from business logic through a standard logging facade.

**Before:**
No logging at all. When something failed, there was no diagnostic trail.

**After:**
`INFO`-level logging on service operations, `WARN`-level logging on handled errors.

```java
private static final Logger log = LoggerFactory.getLogger(DefaultSatelliteService.class);

log.info("Creating satellite: {}", satellite.getName());
log.warn("Satellite not found: {}", ex.getMessage());
```

**Log levels used:**

| Level | When | Example |
|-------|------|---------|
| `INFO` | Normal operations | "Creating satellite: ISS" |
| `WARN` | Handled error conditions | "Satellite not found: id 42" |

**Why it matters:**
- Provides an operational audit trail without a debugger
- SLF4J placeholder syntax (`{}`) avoids unnecessary string concatenation when logging is disabled
- Makes production issues diagnosable — if clients send repeated bad requests, `WARN` logs make that visible

**Files changed:**
| File | Change |
|------|--------|
| `DefaultSatelliteService.java` | Added `log.info()` on all operations |
| `GlobalExceptionHandler.java` | Added `log.warn()` on error handling |

---

### 5. Move Mapping to DTO — Single Responsibility Principle (SRP)

**Principle:** *"A class should have only one reason to change."*

**Before:**
The controller contained a private `mapToEntity()` method that converted `SatelliteRequest` (DTO) into a `Satellite` (entity). This gave the controller a second responsibility — data transformation on top of HTTP routing.

```java
// Controller was doing mapping work
private Satellite mapToEntity(SatelliteRequest request) {
    SatelliteParameters parameters = new SatelliteParameters(
        request.getParameters().getAlt(), ...
    );
    return new Satellite(request.getName(), ...);
}
```

**After:**
The mapping logic moved into `SatelliteRequest.toEntity()`. The DTO already knows its own fields — it's the natural home for this conversion. The controller becomes a pure routing layer.

```java
// DTO converts itself — it knows its own fields
public class SatelliteRequest {
    public Satellite toEntity() {
        SatelliteParameters params = new SatelliteParameters(
            parameters.getAlt(), parameters.getLat(), parameters.getLon()
        );
        return new Satellite(name, launchDate, orbit, params);
    }
}

// Controller is now a thin routing layer
Satellite created = satelliteService.createSatellite(request.toEntity());
```

**Why it matters:**
- The controller has one job: HTTP routing and delegation
- Conversion logic lives where the data lives (the DTO)
- Symmetry: `SatelliteRequest.toEntity()` converts inward, `SatelliteResponse.fromEntity()` converts outward
- If the mapping becomes more complex, there's one clear place to update

**Files changed:**
| File | Change |
|------|--------|
| `SatelliteRequest.java` | Added `toEntity()` method |
| `SatelliteController.java` | Removed `mapToEntity()`, replaced with `request.toEntity()` |

---

## Summary

After all five improvements, each class has exactly one responsibility:

| Class | Single Responsibility |
|-------|----------------------|
| `SatelliteController` | HTTP routing and delegation |
| `SatelliteRequest` | Input validation + DTO-to-entity conversion |
| `SatelliteResponse` | Entity-to-DTO conversion for API output |
| `ErrorResponse` | Typed error response structure |
| `SatelliteService` | Business operation contract (interface) |
| `DefaultSatelliteService` | Business logic implementation + logging |
| `Satellite` | JPA database mapping |
| `SatelliteRepository` | Data access |
| `GlobalExceptionHandler` | Exception-to-response routing |

All 28 tests pass after every change, verifying that these were pure structural improvements with no behavioural regressions.
