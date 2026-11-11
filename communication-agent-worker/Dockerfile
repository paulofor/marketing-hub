# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -q -DskipTests package
ADD --checksum=sha256:7576310b219e04159d35ff61dd4a4ec4cdba4f35c00e002a136f00e96a908b0a https://github.com/dejavu-fonts/dejavu-fonts/releases/download/version_2_37/dejavu-fonts-ttf-2.37.zip /tmp/iris-fonts.zip
RUN mkdir -p /opt/iris-fonts && cd /opt/iris-fonts && jar -xf /tmp/iris-fonts.zip

FROM eclipse-temurin:21-jre-noble AS java-runtime

FROM mcr.microsoft.com/playwright:v1.54.2-noble@sha256:18b4bcff4f8ba0ac8c44b09f09def6a4f6cb8579e5f26381c21f38b50935d5d8
ARG CODEX_VERSION=0.159.3
# Reutiliza dependências já versionadas, sem consultar índices APT no build.
RUN test -s /etc/ssl/certs/ca-certificates.crt \
    && node --version | grep -Eq '^v2[0-9]\.' \
    && git --version \
    && fc-match sans \
    && npm install -g @openai/codex@${CODEX_VERSION} \
    && npm cache clean --force
WORKDIR /app
COPY --from=build /opt/iris-fonts/dejavu-fonts-ttf-2.37/ttf /usr/share/fonts/truetype/dejavu
COPY --from=build /opt/iris-fonts/dejavu-fonts-ttf-2.37/LICENSE /usr/share/doc/iris-fonts/LICENSE
RUN fc-cache -f && fc-match 'DejaVu Sans' | grep -F 'DejaVuSans.ttf'
COPY --from=java-runtime /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=build /build/target/app.jar /app/app.jar
COPY agent-health-report.mjs /app/agent-health-report.mjs
RUN useradd --create-home --uid 10001 --gid operator operator
USER operator
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
