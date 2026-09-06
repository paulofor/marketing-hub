FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-noble AS java-runtime

FROM mwader/static-ffmpeg:7.1.1@sha256:6769881cc02c80d33e387750a8e144d162adfab2775e934dd97899261dda3a0c AS ffmpeg-runtime

FROM mcr.microsoft.com/playwright:v1.49.0-noble
ARG CODEX_VERSION=0.146.0
WORKDIR /app
COPY --from=java-runtime /opt/java/openjdk /opt/java/openjdk
COPY --from=ffmpeg-runtime /ffmpeg /usr/local/bin/ffmpeg
COPY --from=ffmpeg-runtime /ffprobe /usr/local/bin/ffprobe
COPY package.json package-lock.json /app/
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN node --version | grep -Eq '^v2[0-9]\.' \
    && java -version \
    && npm install -g @openai/codex@${CODEX_VERSION} \
    && npm ci --omit=dev \
    && groupadd --gid 10001 approver \
    && useradd --create-home --uid 10001 --gid approver approver
COPY --from=build /build/target/app.jar /app/app.jar
COPY agent-health-report.mjs /app/agent-health-report.mjs
COPY browser-runtime-check.mjs /app/browser-runtime-check.mjs
COPY src/main/resources/mcp/video-frame-extractor.mjs /app/video-frame-extractor.mjs
COPY src/main/resources/mcp/landing-evidence.mjs /app/landing-evidence.mjs
COPY review-evidence /app/commercial-evidence
USER approver
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
