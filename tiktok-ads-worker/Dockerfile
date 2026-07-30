# syntax=docker/dockerfile:1.6

FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/app.jar ./app.jar

EXPOSE 8097

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
