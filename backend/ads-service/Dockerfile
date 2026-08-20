# syntax=docker/dockerfile:1

# ─────────────────────────────────────────
# Runtime image – receives the JAR already tested and packaged by CI
# ─────────────────────────────────────────
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS ""
ARG BACKEND_BUILD_COMMIT=unknown
ARG BACKEND_BUILD_BRANCH=unknown
ENV BACKEND_BUILD_COMMIT=${BACKEND_BUILD_COMMIT}
ENV BACKEND_BUILD_BRANCH=${BACKEND_BUILD_BRANCH}
WORKDIR /app

# Install curl for healthcheck probes and socat to forward 80 -> 8000 inside the container
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl socat \
    && rm -rf /var/lib/apt/lists/*

# Create directories that will be mounted as volumes
RUN mkdir -p uploads logs

COPY backend/ads-service/target/app-exec.jar /app/app.jar
EXPOSE 8000 80
VOLUME ["/app/uploads", "/app/logs"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -fsS --connect-timeout 2 --max-time 4 http://127.0.0.1:8000/ops-mh-observability-v2/health || exit 1

ENTRYPOINT ["/bin/sh", "-c", "socat TCP-LISTEN:80,fork,reuseaddr TCP:127.0.0.1:8000 & exec java $JAVA_OPTS -jar /app/app.jar"]
