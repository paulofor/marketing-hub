FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-noble
ARG CODEX_VERSION=0.146.0
WORKDIR /app
COPY package.json package-lock.json /app/
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN apt-get update && apt-get install -y --no-install-recommends nodejs npm git ca-certificates \
    && npm install -g @openai/codex@${CODEX_VERSION} \
    && npm ci --omit=dev \
    && npx playwright-core install --with-deps chromium \
    && chmod -R a+rX /ms-playwright \
    && rm -rf /var/lib/apt/lists/* \
    && (getent group operator >/dev/null || groupadd --gid 10001 operator) \
    && (id --user operator >/dev/null 2>&1 || useradd --create-home --uid 10001 --gid operator operator)
COPY --from=build /build/target/app.jar /app/app.jar
COPY --from=build /build/src/main/resources/prompts /app/prompts
COPY --from=build /build/src/main/resources/browser /app/browser
USER operator
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
