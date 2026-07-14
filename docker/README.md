# Container Images

Dockerfiles live here (one folder per target runtime) instead of inline string
literals in code, so they get proper syntax highlighting, linting, and diffs.

```
docker/
├── quarkus/
│   ├── Dockerfile      # multi-stage build-from-source (used by docker-compose)
│   ├── Dockerfile.dev  # dev mode with live reload, source bind-mounted (compose --profile dev)
│   └── Dockerfile.it   # single-stage, packages pre-built fast-jar (Testcontainers)
├── liberty/
│   └── Dockerfile.it   # packages pre-built WAR onto Open Liberty (Testcontainers)
└── helidon/
    └── Dockerfile.it   # packages pre-built fat-jar (Testcontainers)
```

## Who uses what?

| File | Used by | Build context | Inputs it expects |
|---|---|---|---|
| `docker/quarkus/Dockerfile` | `docker-compose.yml` (service `confapi`) | repo root | full source tree (does its own `mvn package`) |
| `docker/quarkus/Dockerfile.dev` | `docker-compose.yml` (service `confapi-dev`, profile `dev`) | repo root | source bind-mounted at runtime (runs `mvn quarkus:dev`) |
| `docker/quarkus/Dockerfile.it` | `ConfApiContainer` (IT tests) | a temp dir assembled by Testcontainers | `target/quarkus-app/` |
| `docker/liberty/Dockerfile.it` | `ConfApiContainer` (`-Druntime.profile=liberty`) | temp dir | `target/confapi.war` + `src/main/liberty/config/server.xml` |
| `docker/helidon/Dockerfile.it` | `ConfApiContainer` (`-Druntime.profile=helidon`) | temp dir | `target/confapi.jar` |

## Running

```bash
# App + Jaeger via Compose (uses docker/quarkus/Dockerfile)
docker compose up -d --build

# Dev mode with live reload (uses docker/quarkus/Dockerfile.dev)
docker compose --profile dev up -d --build confapi-dev

# Integration tests (Testcontainers picks the .it Dockerfile by runtime.profile)
mvn verify -Pquarkus
mvn verify -Pliberty -Druntime.profile=liberty
mvn verify -Phelidon -Druntime.profile=helidon
```

