FROM node:22-alpine
WORKDIR /app
COPY package.json ./
COPY src ./src
RUN mkdir -p /data/outbox && chown -R node:node /data /app
USER node
VOLUME ["/data"]
CMD ["node", "src/worker.mjs"]
