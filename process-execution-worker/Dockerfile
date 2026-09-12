FROM node:22-alpine
WORKDIR /app
COPY --chown=node:node package.json ./
COPY --chown=node:node src ./src
USER node
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 CMD node -e "const fs=require('fs');process.exit(Date.now()-Number(fs.readFileSync('/tmp/process-execution-heartbeat','utf8'))<180000?0:1)"
CMD ["node", "src/main.mjs"]
