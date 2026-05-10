FROM maven:3.9.9-eclipse-temurin-21-jammy AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=0

RUN mvn -q -DskipTests package
RUN mvn -q -DskipTests exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps chromium"

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    libnss3 \
    libatk-bridge2.0-0 \
    libxkbcommon0 \
    libgbm1 \
    libasound2 \
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

COPY --from=build /app/target/mois-hotmart-collector.jar /app/app.jar
COPY --from=build /ms-playwright /ms-playwright

# Usa browsers pré-instalados no build; evita tentativa de instalação no runtime
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

EXPOSE 8096
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
