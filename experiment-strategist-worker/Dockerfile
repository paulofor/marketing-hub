FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-noble AS java-runtime

FROM mcr.microsoft.com/playwright:v1.54.2-noble@sha256:18b4bcff4f8ba0ac8c44b09f09def6a4f6cb8579e5f26381c21f38b50935d5d8
ARG CODEX_VERSION=0.146.0
# Compartilha a base e o cliente com os demais agentes sem carregar cache de instalação.
RUN test -s /etc/ssl/certs/ca-certificates.crt \
    && node --version | grep -Eq '^v2[0-9]\.' \
    && npm install -g @openai/codex@${CODEX_VERSION} \
    && npm cache clean --force

WORKDIR /app
COPY --from=java-runtime /opt/java/openjdk /opt/java/openjdk
COPY package.json package-lock.json /app/
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN java -version \
    && git --version \
    && npm ci --omit=dev \
    && npm cache clean --force
COPY --from=build /build/target/app.jar /app/app.jar
COPY agent-health-report.mjs /app/agent-health-report.mjs
COPY --from=build /build/src/main/resources/browser /app/browser
COPY --from=build /build/src/main/resources/mcp /app/mcp
RUN useradd --create-home --uid 10001 strategist \
    && mkdir -p /var/lib/atena \
    && chown strategist:strategist /var/lib/atena
USER strategist
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
