FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -q -DskipTests package

FROM node:20-bookworm-slim AS node-runtime

FROM eclipse-temurin:21-jre-noble
ARG CODEX_VERSION=0.146.0
WORKDIR /app
COPY package.json package-lock.json /app/
COPY --from=node-runtime /usr/local/bin/node /usr/local/bin/node
COPY --from=node-runtime /usr/local/lib/node_modules /usr/local/lib/node_modules
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN ln -s /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
    && ln -s /usr/local/lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx \
    && node --version | grep -Eq '^v2[0-9]\.' \
    && apt-get update && apt-get install -y --no-install-recommends git ca-certificates \
    && npm install -g @openai/codex@${CODEX_VERSION} \
    && npm ci --omit=dev \
    && npx playwright-core install --with-deps chromium \
    && chmod -R a+rX /ms-playwright \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /build/target/app.jar /app/app.jar
COPY agent-health-report.mjs /app/agent-health-report.mjs
COPY --from=build /build/src/main/resources/browser /app/browser
COPY --from=build /build/src/main/resources/mcp /app/mcp
RUN useradd --create-home --uid 10001 strategist
USER strategist
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
