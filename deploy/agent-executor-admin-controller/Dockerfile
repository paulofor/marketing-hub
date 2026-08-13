FROM node:20-alpine

RUN apk add --no-cache docker-cli docker-cli-compose
WORKDIR /app
COPY scripts/agent-executor-admin-controller.mjs /app/controller.mjs
USER root
ENTRYPOINT ["node", "/app/controller.mjs"]
