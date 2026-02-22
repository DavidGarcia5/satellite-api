# Satellite API — Testing & Validation Document

## 1. Testing Strategy

The project employs a two-tier testing strategy to ensure correctness at different levels:

| Layer | Type | Tools | What's Tested |
|-------|------|-------|---------------|
| Service | **Unit Tests** | JUnit 5, Mockito, AssertJ | Business logic in isolation (repository is mocked) |
| Controller | **Integration Tests** | SpringBootTest, MockMvc, H2 | Full request lifecycle — HTTP → Controller → Service → Repository → H2 |

### Why two tiers?

- **Unit tests** run in milliseconds with no Spring context. They verify that the service behaves correctly given specific repository responses (including error cases). Fast feedback during development.
- **Integration tests** boot the full Spring application context with a real H2 database. They verify that everything works together — routing, JSON serialization, validation, error handling, and database operations.

---

## 2. Unit Tests — SatelliteServiceTest

**Location**: `src/test/java/com/satellite/api/service/SatelliteServiceTest.java`

The repository is mocked using `@Mock` / `@InjectMocks`. Tests target `DefaultSatelliteService` (the concrete implementation) while the production controller depends on the `SatelliteService` interface.

| # | Test Name | Method Under Test | Scenario | Expected Outcome |
|---|-----------|-------------------|----------|------------------|
| 1 | `getAllSatellites_returnsList` | `getAllSatellites()` | Repository returns one satellite | Returns list with one element, name is "ISS" |
| 2 | `getAllSatellites_emptyList` | `getAllSatellites()` | Repository returns empty list | Returns empty list (no exception) |
| 3 | `getSatelliteById_found` | `getSatelliteById(1L)` | Repository finds satellite | Returns satellite with correct name and orbit |
| 4 | `getSatelliteById_notFound_throwsException` | `getSatelliteById(99L)` | Repository returns empty Optional | Throws `SatelliteNotFoundException` with ID in message |
| 5 | `createSatellite_savesAndReturns` | `createSatellite(satellite)` | Repository saves successfully | Returns saved satellite with ID, `save()` called once |
| 6 | `updateSatellite_updatesExisting` | `updateSatellite(1L, updated)` | Satellite exists | Fetches existing, saves updated, both methods called |
| 7 | `updateSatellite_notFound_throwsException` | `updateSatellite(99L, satellite)` | Satellite does not exist | Throws `SatelliteNotFoundException`, `save()` never called |
| 8 | `deleteSatellite_deletesExisting` | `deleteSatellite(1L)` | Satellite exists | `deleteById()` called once |
| 9 | `deleteSatellite_notFound_throwsException` | `deleteSatellite(99L)` | Satellite does not exist | Throws `SatelliteNotFoundException`, `deleteById()` never called |
| 10 | `getSatellitePosition_returnsParameters` | `getSatellitePosition(1L)` | Satellite exists | Returns correct alt (408.0), lat (51.6), lon (-0.12) |
| 11 | `getSatellitePosition_notFound_throwsException` | `getSatellitePosition(99L)` | Satellite does not exist | Throws `SatelliteNotFoundException` |

---

## 3. Integration Tests — SatelliteControllerIntegrationTest

**Location**: `src/test/java/com/satellite/api/controller/SatelliteControllerIntegrationTest.java`

Uses `@SpringBootTest` with `@AutoConfigureMockMvc` to test the full HTTP request/response cycle against a real H2 database. Write-operation tests authenticate explicitly using HTTP Basic (`.with(httpBasic("admin", "admin"))`). The database is cleaned and seeded with one satellite (ISS) before each test via `@BeforeEach`.

| # | Test Name | Endpoint | Scenario | Expected Status | Assertions |
|---|-----------|----------|----------|----------------|------------|
| 1 | `getAllSatellites_returns200WithList` | `GET /api/satellites` | One satellite in DB | 200 | JSON array size 1, name is "ISS" |
| 2 | `getSatelliteById_returns200` | `GET /api/satellites/{id}` | Valid ID | 200 | Name "ISS", orbit "LEO", alt 408.0 |
| 3 | `getSatelliteById_notFound_returns404` | `GET /api/satellites/9999` | Non-existent ID | 404 | Error message contains "9999" |
| 4 | `createSatellite_returns201` | `POST /api/satellites` | Valid JSON body | 201 | ID not null, name "Hubble", orbit "LEO" |
| 5 | `createSatellite_invalidInput_returns400` | `POST /api/satellites` | Empty name, null fields | 400 | Validation messages array not empty |
| 6 | `createSatellite_invalidParameters_returns400` | `POST /api/satellites` | lat=999, lon=-999, alt=-100 | 400 | Validation messages array not empty |
| 7 | `updateSatellite_returns200` | `PUT /api/satellites/{id}` | Valid ID + valid body | 200 | Name "ISS Updated", alt 420.0 |
| 8 | `updateSatellite_notFound_returns404` | `PUT /api/satellites/9999` | Non-existent ID | 404 | Error response |
| 9 | `deleteSatellite_returns204` | `DELETE /api/satellites/{id}` | Valid ID | 204 | Subsequent GET returns 404 |
| 10 | `deleteSatellite_notFound_returns404` | `DELETE /api/satellites/9999` | Non-existent ID | 404 | Error response |
| 11 | `getSatellitePosition_returns200` | `GET /api/satellites/{id}/position` | Valid ID | 200 | alt 408.0, lat 51.6, lon -0.12 |
| 12 | `getSatellitePosition_notFound_returns404` | `GET /api/satellites/9999/position` | Non-existent ID | 404 | Error response |
| 13 | `createSatellite_unauthenticated_returns401` | `POST /api/satellites` | No credentials provided | 401 | Unauthorized response |

Write-operation tests (POST, PUT, DELETE) include HTTP Basic credentials on the request to simulate an authenticated user. The security test (test 13) omits credentials to verify that unauthenticated write requests are rejected with a 401.

---

## 4. Validation Rules

Input validation is enforced via Jakarta Bean Validation annotations on the request DTOs. When validation fails, the `GlobalExceptionHandler` returns a 400 response with specific field-level error messages.

### SatelliteRequest

| Field | Type | Annotation | Rule | Error Message |
|-------|------|-----------|------|---------------|
| `name` | String | `@NotBlank` | Must not be null, empty, or whitespace | "Name is required" |
| `launchDate` | LocalDateTime | `@NotNull` | Must be provided | "Launch date is required" |
| `orbit` | OrbitType | `@NotNull` | Must be one of: GEO, MEO, LEO | "Orbit type is required" |
| `parameters` | SatelliteParametersRequest | `@NotNull`, `@Valid` | Must be provided, nested validation triggered | "Parameters are required" |

### SatelliteParametersRequest

| Field | Type | Annotation | Rule | Error Message |
|-------|------|-----------|------|---------------|
| `alt` | double | `@Min(0)` | Altitude must be ≥ 0 km | "Altitude must be >= 0" |
| `lat` | double | `@DecimalMin(-90)`, `@DecimalMax(90)` | Valid latitude range | "Latitude must be >= -90 / <= 90" |
| `lon` | double | `@DecimalMin(-180)`, `@DecimalMax(180)` | Valid longitude range | "Longitude must be >= -180 / <= 180" |

### Database-Level Constraints

In addition to DTO validation, the entity has `@Column(nullable = false)` on `name`, `launchDate`, and `orbit` as a safety net — ensuring data integrity even if validation is bypassed.

---

## 5. Error Response Format

All error responses use the typed `ErrorResponse` Java record with `@JsonInclude(NON_NULL)`, ensuring a consistent JSON structure where null fields are omitted:

### Validation Error (400)

```json
{
    "timestamp": "2026-02-21T12:00:00",
    "status": 400,
    "error": "Validation Failed",
    "messages": [
        "name: Name is required",
        "parameters.lat: Latitude must be <= 90"
    ]
}
```

### Not Found Error (404)

```json
{
    "timestamp": "2026-02-21T12:00:00",
    "status": 404,
    "error": "Not Found",
    "message": "Satellite not found with id: 42"
}
```

---

## 6. Running the Tests

### Run all tests
```bash
./mvnw test
```

### Expected output
```
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Test breakdown
| Test Class | Tests | Type |
|-----------|-------|------|
| `SatelliteApiApplicationTests` | 1 | Context load |
| `SatelliteServiceTest` | 11 | Unit tests |
| `SatelliteControllerIntegrationTest` | 16 | Integration tests |
| **Total** | **28** | |
