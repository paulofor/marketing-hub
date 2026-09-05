FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-noble AS java-runtime

FROM mcr.microsoft.com/playwright:v1.49.0-noble
ARG CODEX_VERSION=0.146.0
WORKDIR /app
COPY --from=java-runtime /opt/java/openjdk /opt/java/openjdk
COPY package.json package-lock.json /app/
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN node --version | grep -Eq '^v2[0-9]\.' \
    && java -version \
    && npm install -g @openai/codex@${CODEX_VERSION} \
    && npm ci --omit=dev \
    && chromium_path="$(find /ms-playwright -type f -path '*/chrome-linux/chrome' | sort | tail -n 1)" \
    && test -n "${chromium_path}" \
    && ln -s "${chromium_path}" /usr/bin/chromium \
    && groupadd --gid 10001 approver \
    && useradd --create-home --uid 10001 --gid approver approver
COPY --from=build /build/target/app.jar /app/app.jar
COPY agent-health-report.mjs /app/agent-health-report.mjs
COPY review-evidence /app/commercial-evidence
USER approver
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
