FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    chromium \
    libnss3 \
    libatk-bridge2.0-0 \
    libxkbcommon0 \
    libgbm1 \
    libasound2t64 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libcups2 \
    libpangocairo-1.0-0 \
    libgtk-3-0 \
    libdrm2 \
    fonts-liberation \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/mois-hotmart-collector.jar app.jar

# Força uso de Chromium do sistema para ambiente Linux headless sem GUI
ENV COLLECTOR_PLAYWRIGHT_EXECUTABLE_PATH=/usr/bin/chromium

EXPOSE 8096
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
