FROM node:20-bookworm-slim

ARG CODEX_VERSION=0.146.0
RUN npm install -g @openai/codex@${CODEX_VERSION}

WORKDIR /app

COPY package.json ./
COPY src ./src
COPY prompts ./prompts

ENV NODE_ENV=production

EXPOSE 8080

CMD ["node", "src/worker.js"]
