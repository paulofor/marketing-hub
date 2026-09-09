FROM mcr.microsoft.com/playwright:v1.54.2-noble@sha256:18b4bcff4f8ba0ac8c44b09f09def6a4f6cb8579e5f26381c21f38b50935d5d8

ARG CODEX_VERSION=0.146.0
# A base já contém certificados, Node e navegadores compatíveis com o lock do módulo.
RUN test -s /etc/ssl/certs/ca-certificates.crt \
    && node --version | grep -Eq '^v2[0-9]\.' \
    && npm install -g @openai/codex@${CODEX_VERSION} \
    && npm cache clean --force

ARG RUNTIME_UID=1000
ARG RUNTIME_GID=1000

WORKDIR /app

ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

COPY package.json package-lock.json ./
RUN npm ci --omit=dev && npm cache clean --force

COPY src ./src
COPY prompts ./prompts
COPY scripts ./scripts
COPY research-library ./research-library
RUN test -s research-library/index.json

ENV NODE_ENV=production

EXPOSE 8080

USER ${RUNTIME_UID}:${RUNTIME_GID}

CMD ["node", "src/worker.js"]
