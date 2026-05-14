# 🛡️ An Opinionated Guide to Bulletproof APIs with Java

> Demo repository for the conference talk *"An Opinionated Guide to Bulletproof APIs with Java"*.

This project demonstrates **5 essential patterns** for building production-grade APIs using **Jakarta EE 11** and **MicroProfile 7** — with zero runtime-specific code. The same WAR runs on Quarkus, Open Liberty, and Helidon.

## 📋 What's Inside

| # | Pattern | Package | Key Tech |
|---|---------|---------|----------|
| 1 | **The Gatekeepers** — Input sanitization, validation, auditing | `gatekeepers` | `ContainerRequestFilter`, `ReaderInterceptor`, `@Valid`, `@NameBinding` |
| 2 | **The Security Shield** — JWT, RBAC, request signatures | `security` | MicroProfile JWT, `@RolesAllowed`, HMAC-SHA256 signature verification |
| 3 | **The Lens** — Observability, tracing, correlation IDs | `observability` | OpenTelemetry, MicroProfile Health, `X-Request-Id` |
| 4 | **The Living Contract** — OpenAPI as source of truth | `openapi` | MicroProfile OpenAPI, `OASFilter` |
| 5 | **The Evolution** — API versioning (URI + header-based) | `versioning`, `resource.v1`, `resource.v2` | `@PreMatching` filter, URI rewriting |
| ⭐ | **Bonus: Sane Error Handling** — RFC 9457 Problem Details | `error` | `ExceptionMapper`, `application/problem+json` |

## 🏗️ Tech Stack

- **Java 21** (LTS)
- **Jakarta EE 11** (Web Profile)
- **MicroProfile 7.0** (JWT, OpenAPI, Health, Config, Telemetry)
- **OpenTelemetry** (tracing)
- **Maven** (build)

## 🚀 Running the Application

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker (required for integration tests, optional for Jaeger traces)

### Option 1: Quarkus

```bash
mvn clean compile quarkus:dev -Pquarkus
```

The app starts at `http://localhost:8080`. Quarkus dev mode enables live reload.

### Option 2: Open Liberty

```bash
mvn clean package liberty:dev -Pliberty
```

Open Liberty dev mode at `http://localhost:8080`.

### Option 3: Helidon

```bash
mvn clean package -Phelidon
java -jar target/confapi.jar
```

### Verify it works

```bash
curl http://localhost:8080/api/v1/sessions | jq
```

## 🔐 JWT Authentication

**Prerequisites:** `openssl` and `uuidgen` (or `/proc/sys/kernel/random/uuid`). Both ship with macOS and most Linux distros. On Windows, run the script under WSL or Git Bash.

Generate test tokens using the included script:

```bash
# Generate an ORGANIZER token (can create/update/delete)
./generate-jwt.sh ORGANIZER

# Generate a SPEAKER token
./generate-jwt.sh SPEAKER

# Generate an ATTENDEE token (read-only)
./generate-jwt.sh ATTENDEE
```

> 🚨 **First-run side effect:** if `/tmp/confapi_private.pem` doesn't exist, the script generates a fresh RSA key pair and **overwrites `src/main/resources/META-INF/publicKey.pem`**. You must rebuild and restart the runtime afterwards so it picks up the new public key. Subsequent runs reuse the existing key and have no side effects.

Use the token from the command line:

```bash
# The JWT is the only line in the script's output that starts with "ey"
# (RS256 tokens always begin with the base64-encoded header {"alg":"RS256",...})
TOKEN=$(./generate-jwt.sh ORGANIZER 2>/dev/null | grep -E '^ey')
curl -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"title":"New Session","abstract":"Description","level":"BEGINNER","speakerId":"spk-duke","startTime":"2026-10-16T11:00:00","durationMinutes":50}' \
     http://localhost:8080/api/v1/sessions
```

Or use it from the [`.http` files](http/): copy the token into [`http/http-client.env.json`](http/http-client.env.json) under `jwt_organizer` / `jwt_speaker` / `jwt_attendee`, then fire requests from any `.http` file.

### Two key pairs — runtime vs. tests

The project uses **two independent RSA key pairs**, on purpose:

| Used by | Private key | Public key (verifier side) |
|---|---|---|
| **Runtime** (live app + `generate-jwt.sh`) | `/tmp/confapi_private.pem` (generated on first script run) | `src/main/resources/META-INF/publicKey.pem` |
| **Integration tests** | `src/test/resources/test-private-key.pem` (committed) | Loaded by the test container via `mp.jwt.verify.publickey.location` from test resources |

This means a token generated with `./generate-jwt.sh` works against the **running app**, not against the test container — and vice versa. If you regenerate the runtime key, tests are unaffected.

## 📊 Observability

Start the Jaeger trace collector:

```bash
docker compose up -d
```

Then make some API calls. View traces at: **http://localhost:16686**

Health checks:

```bash
curl http://localhost:8080/health        # All checks
curl http://localhost:8080/health/live    # Liveness
curl http://localhost:8080/health/ready   # Readiness
```

## 📖 OpenAPI

The OpenAPI spec is auto-generated from code annotations:

```bash
# YAML format
curl http://localhost:8080/openapi

# JSON format
curl http://localhost:8080/openapi?format=json
```

Most runtimes also serve Swagger UI at `/openapi/ui` or `/q/swagger-ui`.

## 🔄 API Versioning

Two strategies running side by side:

```bash
# URI-based (explicit)
curl http://localhost:8080/api/v1/sessions   # Flat DTOs
curl http://localhost:8080/api/v2/sessions   # Enriched with embedded speaker/room

# Header-based (transparent routing)
curl -H "X-API-Version: 2" http://localhost:8080/api/sessions
curl -H "Accept: application/json; version=2" http://localhost:8080/api/sessions
```

## ⚠️ Error Handling

All errors return RFC 9457 Problem Details (`application/problem+json`):

```json
{
  "type": "urn:problem-type:validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "The request body or parameters failed validation.",
  "extensions": {
    "violations": [
      { "field": "title", "message": "Title is required" }
    ]
  }
}
```

## 🌐 Try the API — `.http` files

Ready-to-run requests for every demo live in [`http/`](http/). Open them in JetBrains IDEs or VS Code (REST Client extension) and fire requests one click at a time.

| File | Use it for |
|---|---|
| [`http/demos.http`](http/demos.http) | **Presenter's walkthrough** — every demo from the talk, in slide order |
| [`http/sessions.http`](http/sessions.http), [`speakers.http`](http/speakers.http), [`rooms.http`](http/rooms.http) | Per-resource CRUD reference |
| [`http/security.http`](http/security.http), [`signatures.http`](http/signatures.http) | JWT/RBAC + HMAC signature flows |
| [`http/versioning.http`](http/versioning.http) | URI vs. header-based versioning |
| [`http/health.http`](http/health.http), [`errors.http`](http/errors.http) | Health probes and RFC 9457 Problem Details |

See [`http/README.md`](http/README.md) for the full catalogue, setup steps, and a chapter → file map.

## 📁 Project Structure

```
src/main/java/com/mehmandarov/confapi/
├── ApiApplication.java              # JAX-RS app + OpenAPI + JWT config
├── domain/                          # Entity classes (Session, Speaker, Room)
├── dto/                             # Versioned DTOs (V1 flat, V2 enriched)
├── repository/                      # In-memory stores (ConcurrentHashMap)
├── resource/
│   ├── v1/                          # V1 endpoints (CRUD)
│   └── v2/                          # V2 endpoints (enriched reads)
├── gatekeepers/                     # Ch1: Sanitization, audit, validation, ReaderInterceptor
├── security/                        # Ch2: JWT claims + HMAC signature verification
├── observability/                   # Ch3: Tracing, correlation IDs, health checks
├── openapi/                         # Ch4: OASFilter for OpenAPI enrichment
├── versioning/                      # Ch5: Header-based version routing
└── error/                           # Bonus: RFC 9457 Problem Details mappers

src/test/java/com/mehmandarov/confapi/
├── unit/                            # 48 unit tests (no container, no Docker)
│   ├── Ch1_GatekeepersTest.java     #   Sanitization, ReaderInterceptor, @NoProfanity
│   ├── Ch2_SecurityShieldTest.java  #   HMAC-SHA256, constant-time comparison
│   ├── Ch3_ObservabilityTest.java   #   Correlation ID, health checks
│   ├── Ch5_EvolutionTest.java       #   V1/V2 DTOs, version detection
│   └── Ch6_ErrorHandlingTest.java   #   RFC 9457 ProblemDetail builder
├── support/                         # Test infrastructure (runtime-agnostic)
│   ├── ConfApiContainer.java        #   Singleton Testcontainer (Docker image per runtime)
│   ├── ConfApiExtension.java        #   JUnit 5 extension (starts container, configures REST Assured)
│   └── TestTokens.java              #   Real RS256 JWT generator (nimbus-jose-jwt)
├── Ch1_GatekeepersIT.java           # IT: sanitization, validation, public reads
├── Ch2_SecurityShieldIT.java        # IT: 401/403/201 with real JWT tokens
├── Ch3_ObservabilityIT.java         # IT: health checks, X-Request-Id correlation
├── Ch4_LivingContractIT.java        # IT: OpenAPI spec structure, security scheme, OASFilter
├── Ch5_EvolutionIT.java             # IT: URI + header-based versioning
└── Ch6_ErrorHandlingIT.java         # IT: 404/400/401/403 → RFC 9457 Problem Details
```

## 🧪 Running the Tests

### Prerequisites

- **Java 21+** and **Maven 3.9+** — for all tests
- **Docker** — required for integration tests (Testcontainers builds and runs the app in a container)

> **Colima / non-default Docker socket?** Set `DOCKER_HOST` before running:
> ```bash
> export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
> ```

### Unit Tests Only (48 tests — fast, no Docker)

```bash
mvn test -Pquarkus
```

These run in ~3 seconds. No container, no Docker. Pure JUnit 5.

### Full Suite: Unit + Integration (74 tests)

```bash
mvn verify -Pquarkus
```

This:
1. Compiles and runs 40 unit tests (surefire)
2. Packages the application
3. Builds a Docker image from the build output (Testcontainers)
4. Starts the container, waits for `/api/v1/sessions` to return 200
5. Runs 34 integration tests against the live container (failsafe)
6. Stops the container

### How the Integration Tests Work

The IT tests are **completely runtime-agnostic** — they contain zero Quarkus, Liberty, or Helidon imports. The architecture:

| Component | Role |
|---|---|
| `ConfApiContainer` | Singleton Testcontainer. Builds a Docker image from the Maven build output and starts it once per test run. |
| `ConfApiExtension` | JUnit 5 `@ExtendWith` — starts the container and points REST Assured at its dynamic port. |
| `TestTokens` | Generates **real RS256 JWT tokens** (signed with `test-private-key.pem`) for security tests. The container validates them through the standard MicroProfile JWT pipeline — no mocks. |

To switch runtimes, only the Docker image builder changes — the tests stay identical:
```bash
mvn verify -Pquarkus                       # Quarkus (default)
mvn verify -Pliberty -Druntime.profile=liberty   # Open Liberty (future)
mvn verify -Phelidon -Druntime.profile=helidon   # Helidon (future)
```

### Test Isolation vs. Startup Time

The integration tests share a **single container** across all 6 IT classes. This keeps the total IT run under 10 seconds (container starts once in ~3 s, then 34 tests run against it).

**Trade-off:** tests share mutable state. A session created in `Ch2_SecurityShieldIT` is visible to later tests. This is acceptable for a demo API with seed data, but if full isolation is required, replace the singleton in `ConfApiContainer` with a per-class container — at the cost of ~3 s startup per IT class (~18 s total instead of ~7 s).

### Test Counts

| Phase | Tests | Docker? |
|-------|-------|---------|
| Unit (`mvn test`) | 40 | No |
| Integration (`mvn verify`) | 34 | Yes |
| **Total** | **74** | |

## 📝 License

Apache 2.0

