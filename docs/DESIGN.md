# Satellite API — Design Document

## 1. Overview

A RESTful API for managing satellite data, built as a technical demonstrator. The system supports full CRUD operations on satellite entities and provides positional data retrieval, typical of SATCOM applications.

### Tech Stack

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| Framework | Spring Boot 4.0.3 | Industry-standard for Java REST APIs |
| Language | Java 21 | Latest LTS, modern language features (text blocks, records) |
| Build | Maven | Dependency management and build lifecycle |
| Database | H2 (in-memory) | Zero-config, ideal for development and testing |
| ORM | Hibernate / Spring Data JPA | Eliminates boilerplate data access code |
| Validation | Jakarta Bean Validation | Declarative input validation |
| Documentation | SpringDoc OpenAPI (Swagger UI) | Interactive API exploration |
| Testing | JUnit 5, Mockito, MockMvc | Unit and integration test coverage |
| Logging | SLF4J / Logback | Structured operational logging |
| Security | Spring Security (HTTP Basic) | Authentication and authorization |

---

## 2. Architecture

The application follows a **layered architecture** pattern, separating concerns across four layers:

```
┌─────────────────────────────────────────┐
│              Controller Layer           │  ← HTTP request/response handling
│         (SatelliteController)           │
├─────────────────────────────────────────┤
│               Service Layer             │  ← Business logic
│   (SatelliteService interface /         │
│    DefaultSatelliteService)             │
├─────────────────────────────────────────┤
│             Repository Layer            │  ← Data access
│        (SatelliteRepository)            │
├─────────────────────────────────────────┤
│              Database (H2)              │  ← Persistence
└─────────────────────────────────────────┘
```

**Why layered architecture?**
- **Separation of concerns** — each layer has a single responsibility
- **Testability** — the service layer can be unit tested with a mocked repository
- **Maintainability** — changes to one layer don't ripple through the others
- **Flexibility** — the H2 database could be swapped for PostgreSQL with zero changes to the service or controller layers
- **Dependency Inversion** — the controller depends on the `SatelliteService` interface, not the concrete implementation

---

## 3. Domain Model

Based on the class diagram provided by the systems engineering team:

```
┌──────────────────────────────────┐       ┌─────────────────────┐
│       Satellite                  │       │  SatelliteParameters│
├──────────────────────────────────┤       ├─────────────────────┤
│ - id: Long (generated)           │       │ - alt: double       │
│ - name: String                   │  1:1  │ - lat: double       │
│ - launchDate: LocalDateTime      ├─────► │ - lon: double       │
│ - orbit: OrbitType               │       └─────────────────────┘
│ - parameters: SatelliteParameters│
└──────────────┬───────────────────┘
               │
       ┌───────┴───────┐
       │  «enumeration»│
       │   OrbitType   │
       ├───────────────┤
       │ GEO           │
       │ MEO           │
       │ LEO           │
       └───────────────┘
```

### Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| SatelliteParameters mapping | `@Embeddable` (same table) | The 1:1 composition is simple; a separate table would add unnecessary JOIN overhead |
| OrbitType storage | `@Enumerated(EnumType.STRING)` | Stores `"GEO"`, `"MEO"`, `"LEO"` as text — safe against enum reordering (unlike `ORDINAL`) |
| ID generation | `GenerationType.IDENTITY` | Auto-incrementing, database-managed primary key |
| Field naming | `lon` instead of `long` | `long` is a reserved keyword in Java |

---

## 4. API Endpoints

Base URL: `http://localhost:8080/api/satellites`

| Method | Endpoint | Status Codes | Auth Required | Description |
|--------|----------|-------------|---------------|-------------|
| `GET` | `/api/satellites` | 200 | No | Retrieve all satellites |
| `GET` | `/api/satellites/{id}` | 200, 404 | No | Retrieve a satellite by ID |
| `POST` | `/api/satellites` | 201, 400, 401 | **Yes** | Add a new satellite |
| `PUT` | `/api/satellites/{id}` | 200, 400, 401, 404 | **Yes** | Update an existing satellite |
| `DELETE` | `/api/satellites/{id}` | 204, 401, 404 | **Yes** | Delete a satellite by ID |
| `GET` | `/api/satellites/{id}/position` | 200, 404 | No | Retrieve satellite position |

### Example Request — Create Satellite

```http
POST /api/satellites
Content-Type: application/json

{
    "name": "ISS",
    "launchDate": "1998-11-20T06:40:00",
    "orbit": "LEO",
    "parameters": {
        "alt": 408.0,
        "lat": 51.6,
        "lon": -0.12
    }
}
```

### Example Response — 201 Created

```json
{
    "id": 1,
    "name": "ISS",
    "launchDate": "1998-11-20T06:40:00",
    "orbit": "LEO",
    "parameters": {
        "alt": 408.0,
        "lat": 51.6,
        "lon": -0.12
    }
}
```

### Example Response — Position

```http
GET /api/satellites/1/position
```

```json
{
    "alt": 408.0,
    "lat": 51.6,
    "lon": -0.12
}
```

---

## 5. DTOs and Validation

The API uses dedicated DTOs for both **input** and **output**, fully decoupling the API contract from the JPA entity:

| DTO | Direction | Purpose |
|-----|-----------|--------|
| `SatelliteRequest` | Input | Validates client data, converts to entity via `toEntity()` |
| `SatelliteParametersRequest` | Input | Nested validation for position parameters |
| `SatelliteResponse` | Output | Defines the JSON response shape via `fromEntity()` |
| `ErrorResponse` | Output | Typed, immutable error response (Java record) |

This provides:

- **Controlled input** — clients cannot set the `id` field
- **Decoupled API contract** — the entity can evolve independently of the API
- **Clear validation** — constraints are defined on the DTO, not cluttering the entity
- **Type-safe errors** — `ErrorResponse` replaces raw `HashMap<String, Object>`, with `@JsonInclude(NON_NULL)` to hide unused fields

### Validation Rules

| Field | Constraint | Error Message |
|-------|-----------|---------------|
| `name` | `@NotBlank` | "Name is required" |
| `launchDate` | `@NotNull` | "Launch date is required" |
| `orbit` | `@NotNull` | "Orbit type is required" |
| `parameters` | `@NotNull`, `@Valid` | "Parameters are required" |
| `parameters.alt` | `>= 0` | "Altitude must be >= 0" |
| `parameters.lat` | `-90` to `90` | "Latitude must be >= -90 / <= 90" |
| `parameters.lon` | `-180` to `180` | "Longitude must be >= -180 / <= 180" |

---

## 6. Error Handling

A `@RestControllerAdvice` (`GlobalExceptionHandler`) intercepts exceptions and returns consistent, typed `ErrorResponse` records.

Null fields are excluded from the response via `@JsonInclude(NON_NULL)` — a 404 error won't include `"messages": null`, and a 400 validation error won't include `"message": null`.

### 404 — Satellite Not Found

```json
{
    "timestamp": "2026-02-21T12:00:00",
    "status": 404,
    "error": "Not Found",
    "message": "Satellite not found with id: 42"
}
```

### 400 — Validation Error

```json
{
    "timestamp": "2026-02-21T12:00:00",
    "status": 400,
    "error": "Validation Failed",
    "messages": [
        "name: Name is required",
        "orbit: Orbit type is required",
        "parameters.lat: Latitude must be <= 90"
    ]
}
```

---

## 7. Transaction Management

Service methods are annotated with `@Transactional` to ensure database operations are atomic:

| Method | Annotation | Why |
|--------|-----------|-----|
| `getAllSatellites()` | `@Transactional(readOnly = true)` | Optimises Hibernate (no dirty checking) |
| `getSatelliteById()` | `@Transactional(readOnly = true)` | Read-only optimisation |
| `getSatellitePosition()` | `@Transactional(readOnly = true)` | Read-only optimisation |
| `createSatellite()` | `@Transactional` | Ensures save is atomic |
| `updateSatellite()` | `@Transactional` | Find + save runs as single atomic operation |
| `deleteSatellite()` | `@Transactional` | Check + delete runs as single atomic operation |

---

## 8. API Documentation

Interactive API documentation is available via Swagger UI:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

All endpoints can be tested directly from the browser using the "Try it out" feature.

Each endpoint documents its possible status codes and error response schemas via `@ApiResponse` annotations, so Swagger UI shows the exact response shape for success and error cases.

---

## 9. Security

The API uses **Spring Security** with **HTTP Basic authentication** and a stateless session policy.

### Access Rules

| Resource | Access |
|----------|--------|
| `GET /api/satellites/**` | Public — no authentication needed |
| `POST /PUT /DELETE /api/satellites/**` | Requires authentication |
| Swagger UI, OpenAPI docs | Public |
| H2 Console | Public |

### Configuration

- **CSRF disabled** — appropriate for a stateless REST API (no browser forms)
- **Stateless sessions** — no server-side session; credentials are sent with each request
- **BCrypt password encoding** — passwords are hashed, never stored in plain text
- **In-memory user** — a demo user (`admin` / `admin`) is configured for development

### Testing with cURL

```bash
# Public — no credentials needed
curl http://localhost:8080/api/satellites

# Authenticated — HTTP Basic credentials required
curl -u admin:admin -X POST http://localhost:8080/api/satellites \
  -H "Content-Type: application/json" \
  -d '{"name":"NewSat","launchDate":"2026-01-01T00:00:00","orbit":"LEO","parameters":{"alt":500,"lat":0,"lon":0}}'
```

---

## 10. Data Seeding

The application seeds the database with 3 sample satellites on startup via a `CommandLineRunner` (`DataSeeder`). This allows the API to be explored immediately without manually creating entries.

| Satellite | Orbit | Altitude |
|-----------|-------|----------|
| ISS | LEO | 408 km |
| Hubble Space Telescope | LEO | 547 km |
| GPS IIF-1 | MEO | 20,200 km |

The seeder checks `count() > 0` before inserting, so it won't duplicate data if restarted.

---

## 11. Project Structure

```
com.satellite.api
├── SatelliteApiApplication.java          # Application entry point
├── config/
│   ├── DataSeeder.java                   # Seeds sample data on startup
│   └── SecurityConfig.java               # Authentication & authorization rules
├── controller/
│   └── SatelliteController.java          # REST endpoints (thin routing layer)
├── dto/
│   ├── SatelliteRequest.java             # Input DTO with validation + toEntity()
│   ├── SatelliteParametersRequest.java   # Nested parameters input DTO
│   ├── SatelliteResponse.java            # Output DTO with fromEntity()
│   └── ErrorResponse.java               # Typed error response (Java record)
├── exception/
│   ├── SatelliteNotFoundException.java   # Custom 404 exception
│   └── GlobalExceptionHandler.java       # Centralized error handling + logging
├── model/
│   ├── Satellite.java                    # JPA entity (persistence only)
│   ├── SatelliteParameters.java          # Embeddable position data
│   └── OrbitType.java                    # Orbit type enum
├── repository/
│   └── SatelliteRepository.java          # Spring Data JPA interface
└── service/
    ├── SatelliteService.java             # Service interface (contract)
    └── DefaultSatelliteService.java      # Service implementation + logging
```

---

## 12. Running the Application

### Prerequisites
- Java 21+
- Maven (or use the included Maven wrapper)

### Start the application
```bash
./mvnw spring-boot:run
```

### Access points
- API: `http://localhost:8080/api/satellites`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:satellitedb`, user: `sa`, no password)

### Run tests
```bash
./mvnw test
```
