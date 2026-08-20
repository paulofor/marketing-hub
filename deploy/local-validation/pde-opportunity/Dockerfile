FROM node:20-bookworm-slim

WORKDIR /app
COPY run-agent.mjs ./run-agent.mjs
COPY contract.mjs ./contract.mjs
COPY prompts ./prompts
COPY schemas ./schemas

ENTRYPOINT ["node", "/app/run-agent.mjs"]
