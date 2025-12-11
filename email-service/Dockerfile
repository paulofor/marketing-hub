# syntax=docker/dockerfile:1.4

FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY backend/settings.xml backend/settings.xml
COPY backend/email-service/pom.xml backend/email-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -f backend/email-service/pom.xml -s backend/settings.xml dependency:go-offline

COPY backend/email-service/src backend/email-service/src
RUN --mount=type=cache,target=/root/.m2 mvn -f backend/email-service/pom.xml -s backend/settings.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /workspace/backend/email-service/target/app.jar /app/app.jar
EXPOSE 8080

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
