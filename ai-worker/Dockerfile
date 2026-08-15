# syntax=docker/dockerfile:1.6

########################################
# Build stage
########################################
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

ARG GITHUB_ACTOR
ARG GITHUB_TOKEN
ENV GITHUB_ACTOR=${GITHUB_ACTOR}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

# Instala os contratos do backend desta mesma revisão. O worker não pode compilar
# contra o pacote publicado anteriormente quando o contrato muda no mesmo lote.
COPY backend/ads-service ./backend/ads-service
RUN mvn -f backend/ads-service/pom.xml -B -Dmaven.test.skip=true install

# Copy the application sources and build
COPY ai-worker ./ai-worker
RUN mvn -f ai-worker/pom.xml -s ai-worker/settings.xml -B package -DskipTests

########################################
# Runtime stage
########################################
FROM mcr.microsoft.com/playwright/java:v1.49.0-jammy
WORKDIR /app

COPY --from=build /workspace/ai-worker/target/app.jar ./app.jar
COPY ai-worker/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

EXPOSE 4567

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
