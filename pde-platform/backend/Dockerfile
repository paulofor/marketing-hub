FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY scripts/maven-build-with-transient-retry.sh ./scripts/maven-build-with-transient-retry.sh
RUN bash ./scripts/maven-build-with-transient-retry.sh \
    -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN bash ./scripts/maven-build-with-transient-retry.sh \
    -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/pde-platform-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8096
ENTRYPOINT ["java", "-jar", "app.jar"]
