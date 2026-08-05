FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
ARG CODEX_VERSION=0.146.0
RUN apt-get update && apt-get install -y --no-install-recommends nodejs npm git ca-certificates \
    && npm install -g @openai/codex@${CODEX_VERSION} \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 operator \
    && useradd --create-home --uid 10001 --gid operator operator
WORKDIR /app
COPY --from=build /build/target/app.jar /app/app.jar
COPY --from=build /build/src/main/resources/prompts /app/prompts
USER operator
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
