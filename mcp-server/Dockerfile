# syntax=docker/dockerfile:1.4

FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY settings.xml ./settings.xml
COPY pom.xml ./pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -s settings.xml -f pom.xml dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -s settings.xml -f pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /workspace/target/app-exec.jar /app/app.jar
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 8096
ENV JAVA_OPTS=""
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
