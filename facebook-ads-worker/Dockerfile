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

COPY pom.xml settings.xml ./
RUN mvn -s settings.xml -B dependency:go-offline

COPY src ./src
RUN mvn -s settings.xml -B package -DskipTests

########################################
# Runtime stage
########################################
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/app.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
