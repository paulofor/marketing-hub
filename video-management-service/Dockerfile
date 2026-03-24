# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY video-management-service/pom.xml video-management-service/pom.xml
RUN mvn -B -f video-management-service/pom.xml dependency:go-offline

COPY video-management-service/src video-management-service/src
RUN mvn -B -f video-management-service/pom.xml package -DskipTests && \
    mv video-management-service/target/video-management-service-0.0.1-SNAPSHOT.jar video-management-service/target/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/video-management-service/target/app.jar app.jar
EXPOSE 8095
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8095/actuator/health || exit 1
ENTRYPOINT ["/bin/sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
