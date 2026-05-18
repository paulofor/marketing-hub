# syntax=docker/dockerfile:1

# ─────────────────────────────────────────
# Build stage – compiles the Spring Boot app
# ─────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy files required to cache Maven dependencies first
COPY backend/settings.xml backend/settings.xml
COPY backend/ads-service/pom.xml backend/ads-service/pom.xml
RUN mvn -B -s backend/settings.xml -f backend/ads-service/pom.xml dependency:go-offline

# Copy the full source tree and build the executable JAR
COPY backend/ads-service backend/ads-service
RUN mvn -B -s backend/settings.xml -f backend/ads-service/pom.xml package -DskipTests \
    && mv backend/ads-service/target/app-exec.jar backend/ads-service/target/app.jar

# ─────────────────────────────────────────
# Runtime stage – lightweight JRE with the packaged app
# ─────────────────────────────────────────
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS ""
WORKDIR /app

# Install curl for healthcheck probes and socat to forward 80 -> 8000 inside the container
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl socat \
    && rm -rf /var/lib/apt/lists/*

# Create directories that will be mounted as volumes
RUN mkdir -p uploads logs

COPY --from=build /workspace/backend/ads-service/target/app.jar /app/app.jar
EXPOSE 8000 80
VOLUME ["/app/uploads", "/app/logs"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8000/ops-mh-observability-v2/health || exit 1

ENTRYPOINT ["/bin/sh", "-c", "socat TCP-LISTEN:80,fork,reuseaddr TCP:127.0.0.1:8000 & exec java $JAVA_OPTS -jar /app/app.jar"]
