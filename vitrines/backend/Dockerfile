# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace/app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/app/target/vitrines-backend-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
