# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
ARG BUILD_REVISION=unknown
LABEL org.opencontainers.image.title="Marketing Hub Harness Library API" \
      org.opencontainers.image.revision="${BUILD_REVISION}" \
      org.opencontainers.image.source="https://github.com/paulofor/marketing-hub"
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/app.jar /app/app.jar
USER app
EXPOSE 8103
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -fsS http://127.0.0.1:9103/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
