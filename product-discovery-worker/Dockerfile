FROM node:20-bookworm-slim

WORKDIR /app

COPY package.json ./
COPY src ./src
COPY prompts ./prompts

ENV NODE_ENV=production

CMD ["node", "src/worker.js"]
