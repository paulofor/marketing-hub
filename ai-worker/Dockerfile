# syntax=docker/dockerfile:1.6

# Runtime Node/Codex é versionado; a imagem final conserva o navegador Java já homologado.
FROM node:22-bookworm-slim AS codex-runtime
ARG CODEX_VERSION=0.159.3
RUN npm install -g @openai/codex@${CODEX_VERSION} && npm cache clean --force
WORKDIR /codex
COPY codex/package*.json ./
RUN npm ci --omit=dev --ignore-scripts && npm cache clean --force

# Recebe somente o JAR da mesma revisão já compilado e aprovado pelo job de testes.
FROM mcr.microsoft.com/playwright/java:v1.49.0-jammy
WORKDIR /app
COPY --from=codex-runtime /usr/local/bin/node /usr/local/bin/node
COPY --from=codex-runtime /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -s /usr/local/lib/node_modules/@openai/codex/bin/codex.js /usr/local/bin/codex \
    && test -s /etc/ssl/certs/ca-certificates.crt && flock --version && codex --version
COPY --from=codex-runtime /codex/node_modules /app/codex/node_modules
COPY codex/*.mjs /app/codex/

COPY target/app.jar ./app.jar
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
ENV CODEX_HOME=/var/lib/ai-worker/codex-home
ENV AI_WORKER_CODEX_WORKSPACE=/tmp/ai-worker-codex

EXPOSE 4567

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
