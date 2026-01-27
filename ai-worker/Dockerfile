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

# Pre-copy Maven config and dependency descriptors
COPY pom.xml settings.xml ./
RUN mvn -s settings.xml -B dependency:go-offline

# Copy the application sources and build
COPY src ./src
RUN mvn -s settings.xml -B package -DskipTests

########################################
# Runtime stage
########################################
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/app.jar ./app.jar
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

EXPOSE 4567

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
