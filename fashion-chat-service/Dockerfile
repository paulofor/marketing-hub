# syntax=docker/dockerfile:1.7

FROM node:20-alpine AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci --include=dev

FROM deps AS builder
COPY tsconfig.json ./
COPY src ./src
COPY tests ./tests
RUN npm run build

FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
ENV CODEX_HOME=/var/lib/ai-hub/codex
COPY package*.json ./
RUN npm ci --omit=dev \
  && npm install -g @openai/codex@0.141.0 \
  && codex --version \
  && mkdir -p /var/lib/ai-hub/codex \
  && chmod 700 /var/lib/ai-hub/codex \
  && npm cache clean --force
COPY --from=builder /app/dist ./dist
EXPOSE 8094
CMD ["node", "dist/src/index.js"]
