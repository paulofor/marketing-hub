# syntax=docker/dockerfile:1

# ─────────────────────────────────────────
# Build stage – compiles the Vite application
# ─────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
ARG VITE_FASHION_CHAT_SERVICE_URL=http://191.252.210.83:8094
ENV VITE_FASHION_CHAT_SERVICE_URL=${VITE_FASHION_CHAT_SERVICE_URL}
RUN npm run build

# ─────────────────────────────────────────
# Runtime stage – serves the static bundle via Nginx
# ─────────────────────────────────────────
FROM nginx:1.27-alpine

# Replace default server configuration to keep the public port at 5173
COPY frontend/docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 5173
CMD ["nginx", "-g", "daemon off;"]
