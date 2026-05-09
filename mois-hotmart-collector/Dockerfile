FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
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

# Pre-instala Chromium do Playwright no build para evitar download em runtime
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN mkdir -p /ms-playwright \
    && java -cp /app/app.jar com.microsoft.playwright.CLI install chromium \
    && test -d /ms-playwright \
    && find /ms-playwright -maxdepth 4 -type f \( -name chrome -o -name chromium \)

EXPOSE 8096
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
