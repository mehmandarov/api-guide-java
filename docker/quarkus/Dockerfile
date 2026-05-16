# syntax=docker/dockerfile:1.7
# ============================================================
# Conference API — Container image (Quarkus, JVM fast-jar)
# ============================================================
# Build from source, multi-stage. Used by docker-compose.
#
# Build context: project root (compose sets build.context: .)
# Build:   docker build -f docker/quarkus/Dockerfile -t confapi:local .
# Run:     docker run --rm -p 8080:8080 confapi:local
# Compose: docker compose up -d --build
# ============================================================

# --- Build stage -------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Prime the dependency cache (better layer reuse on source-only changes)
COPY pom.xml ./
RUN mvn -B -Pquarkus -q -DskipTests dependency:go-offline || true

# Build the application
COPY src ./src
RUN mvn -B -Pquarkus -DskipTests package

# --- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copy Quarkus fast-jar layout produced by the build above
COPY --from=build /workspace/target/quarkus-app/ ./

# Run as a non-root user
RUN useradd --system --uid 1001 --home-dir /app --shell /usr/sbin/nologin app \
 && chown -R app:app /app
USER app

EXPOSE 8080

# Bind to all interfaces inside the container
ENV QUARKUS_HTTP_HOST=0.0.0.0

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]

