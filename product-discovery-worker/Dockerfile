FROM node:20-bookworm-slim

ARG CODEX_VERSION=0.146.0
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && update-ca-certificates \
    && npm install -g @openai/codex@${CODEX_VERSION} \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY package.json ./
COPY src ./src
COPY prompts ./prompts
COPY scripts ./scripts
COPY research-library ./research-library

ENV NODE_ENV=production

EXPOSE 8080

CMD ["node", "src/worker.js"]
