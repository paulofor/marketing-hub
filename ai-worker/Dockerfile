# syntax=docker/dockerfile:1.6

# Runtime image: recebe o JAR da mesma revisão já compilado e aprovado pelo job de testes.
FROM mcr.microsoft.com/playwright/java:v1.49.0-jammy
WORKDIR /app

COPY target/app.jar ./app.jar
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

EXPOSE 4567

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
