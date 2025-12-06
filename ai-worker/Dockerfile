# syntax=docker/dockerfile:1.6

########################################
# Build stage
########################################
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

# Pre-copy Maven config and dependency descriptors
COPY pom.xml settings.xml ./
COPY lib ./lib
RUN mvn -s settings.xml install:install-file \
    -Dfile=lib/ads-service.jar \
    -DgroupId=com.marketinghub \
    -DartifactId=ads-service \
    -Dversion=0.0.1-SNAPSHOT \
    -Dpackaging=jar \
    -DgeneratePom=true
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

EXPOSE 4567

ENTRYPOINT ["java","-jar","/app/app.jar"]
